package com.skitsanos.api;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skitsanos.api.exceptions.CouchDbException;
import com.skitsanos.api.exceptions.InvalidServerResponseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

@SuppressWarnings("unchecked")
public class CouchDb extends ConnectionBase {
	private static final String INVALID_DATABASE_NAME = "Invalid Database name";
	private static final String INVALID_DB_NAME_DOCID = "Invalid Database name / Doc / Design Id";
	private static final String APPLICATION_JSON = "application/json";
	private static final String INVALID_ARGUMENTS = "Invalid arguments";
	private static final String INVALID_SERVER_RESPONSE = "Invalid Server Response!";
	private ObjectMapper mapper = new ObjectMapper();

	private boolean isStringEmptyOrNull(String param) {
		if ((null != param) && (param.length() > 0))
			return false;
		return true;
	}

	public CouchDb(String host, int port) {
		if (host.startsWith("http://"))
			host = host.replace("http://", "");
		if (host.startsWith("https://")) {
			host = host.replace("https://", "");
			useSsl(true);
		}
		if (host.contains("cloudant.com")) {
			useSsl(true);
		}
		this.host(host);
		this.port(port);
	}

	public CouchDb(String host, int port, String userName, String password) {
		if (host.startsWith("http://"))
			host = host.replace("http://", "");
		if (host.startsWith("https://")) {
			host = host.replace("https://", "");
			useSsl(true);
		}
		if (host.contains("cloudant.com")) {
			useSsl(true);
		}
		this.host(host);
		this.port(port);
		this.username(userName);
		this.password(password);
	}

	public String version() throws InvalidServerResponseException, CouchDbException {
		ServerResponse response = doRequest(getUrl() + "/", "GET", false);
		String version = "";
		if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
			try {
				Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
				});
				version = (String) responseMap.get("version");
			} catch (JsonParseException | JsonMappingException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else {
			throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
		}
		return version;
	}

	public ServerResponse getActiveTasks() {
		ServerResponse response = doRequest(getUrl() + "/_active_tasks", "GET", false);
		return response;
	}

	public ServerResponse getDatabases() {
		ServerResponse response = doRequest(getUrl() + "/_all_dbs", "GET", false);
		return response;
	}

	public boolean databaseExists(String db) throws CouchDbException, InvalidServerResponseException {
		boolean exists = false;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db)) {
			response = doRequest(getUrl() + "/" + db.toLowerCase(), "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (!responseMap.containsKey("error"))
						exists = true;
				} catch (JsonParseException | JsonMappingException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException("Invalid Server Response! " + response.data());
			}

		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return exists;
	}

	public ServerResponse createDatabase(String db) throws CouchDbException {
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db)) {
			response = doRequest(getUrl() + "/" + db.toLowerCase(), "PUT", false);
			try {
				Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
				});
				if (responseMap.containsKey("error"))
					throw new CouchDbException("Database creation failed - " + response.data());
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return response;
	}

	public ServerResponse deleteDatabase(String db) throws CouchDbException {
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db)) {
			response = doRequest(getUrl() + "/" + db.toLowerCase(), "DELETE", false);
			try {
				Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
				});
				if (responseMap.containsKey("error"))
					throw new CouchDbException("Database deletion failed - " + response.data());
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return response;
	}

	public ServerResponse replicate(String content) throws CouchDbException, InvalidServerResponseException {
		if (isStringEmptyOrNull(content))
			throw new CouchDbException(INVALID_ARGUMENTS);

		ServerResponse response = null;
		response = doRequest(getUrl() + "/_replicate", "POST", content, APPLICATION_JSON, false);
		if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
			try {
				Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
				});
				if (responseMap.containsKey("error"))
					throw new CouchDbException("Replication failed - " + response.data());
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else {
			throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
		}
		return response;
	}

	public ServerResponse replicate(Object content) throws CouchDbException, InvalidServerResponseException {
		if (null == content)
			throw new CouchDbException(INVALID_ARGUMENTS);
		try {
			return replicate(mapper.writeValueAsString(content));
		} catch (IOException e) {
			throw new CouchDbException("Exception - " + e.getClass().getName() + " - " + e.getMessage());
		}
	}

	public List<DocumentInfo> getAllDocuments(String db) throws CouchDbException, InvalidServerResponseException {
		List<DocumentInfo> docList = new ArrayList<DocumentInfo>();
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db)) {
			response = doRequest(getUrl() + "/" + db + "/_all_docs", "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());

					List<Map<String, Object>> rows = (ArrayList<Map<String, Object>>) responseMap.get("rows");
					for (Map<String, Object> row : rows) {
						DocumentInfo doc = new DocumentInfo();
						doc.id = (String) row.get("id");
						if (row.containsKey("value")) {
							Map<String, Object> revision = (Map<String, Object>) row.get("value");
							if (revision.containsKey("rev"))
								doc.revision = (String) revision.get("rev");
						}

						docList.add(doc);
					}
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return docList;
	}

	public int countDocuments(String db) throws CouchDbException, InvalidServerResponseException {
		int docCount = -1;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db)) {
			response = doRequest(getUrl() + "/" + db, "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					if (responseMap.containsKey("doc_count"))
						docCount = (Integer) responseMap.get("doc_count");

				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return docCount;
	}

	public boolean documentExists(String db, String docId) throws CouchDbException, InvalidServerResponseException {
		boolean docExists = false;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId)) {
			response = doRequest(getUrl() + String.format("/%s/%s", db, docId), "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					docExists = responseMap.containsKey("id");
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return docExists;
	}

	public DocumentInfo createDocument(String db, String content) throws CouchDbException, InvalidServerResponseException {
		DocumentInfo doc = null;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(content)) {
			response = doRequest(getUrl() + "/" + db, "POST", content, "application/json", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					if (responseMap.containsKey("ok") && ((boolean) responseMap.get("ok"))) {
						doc = new DocumentInfo();
						doc.id = (String) responseMap.get("id");
						doc.revision = (String) responseMap.get("rev");
					}

				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return doc;
	}

	public DocumentInfo createDocument(String db, Object content) throws CouchDbException, InvalidServerResponseException {
		if (null == content)
			throw new CouchDbException(INVALID_ARGUMENTS);
		try {
			return createDocument(db, mapper.writeValueAsString(content));
		} catch (IOException e) {
			throw new CouchDbException("Exception - " + e.getClass().getName() + " - " + e.getMessage());
		}
	}

	public String getDocumentAsJson(String db, String docId) throws CouchDbException, InvalidServerResponseException {
		String data = null;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId)) {
			response = doRequest(getUrl() + String.format("/%s/%s", db, docId), "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					data = response.data();
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return data;
	}

	public DocumentInfo updateDocument(String db, String docId, String content) throws CouchDbException, InvalidServerResponseException {
		DocumentInfo doc = null;
		ServerResponse response = null;
		String docContent = getDocumentAsJson(db, docId);
		String docRev = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId) && !isStringEmptyOrNull(content)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(docContent, new TypeReference<Map<String, Object>>() {
				});
				if (documentMap.containsKey("_rev"))
					docRev = (String) documentMap.get("_rev");
				Map<String, Object> newContentMap = mapper.readValue(content, new TypeReference<Map<String, Object>>() {
				});

				newContentMap.put("_id", docId);
				newContentMap.put("_rev", docRev);
				content = mapper.writeValueAsString(newContentMap);
				response = doRequest(getUrl() + "/" + db, "POST", content, "application/json", false);
				if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
					try {
						Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
						});
						if (responseMap.containsKey("error"))
							throw new CouchDbException("Errored - " + response.data());
						if (responseMap.containsKey("ok") && ((boolean) responseMap.get("ok"))) {
							doc = new DocumentInfo();
							doc.id = (String) responseMap.get("id");
							doc.revision = (String) responseMap.get("rev");
						}

					} catch (IOException e) {
						throw new CouchDbException("Exception - " + e.getMessage());
					}
				} else {
					throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return doc;
	}

	public DocumentInfo updateDocument(String db, String docId, Object content) throws CouchDbException, InvalidServerResponseException {
		if (null == content)
			throw new CouchDbException(INVALID_ARGUMENTS);
		try {
			return updateDocument(db, docId, mapper.writeValueAsString(content));
		} catch (IOException e) {
			throw new CouchDbException("Exception - " + e.getClass().getName() + " - " + e.getMessage());
		}
	}

	public DocumentInfo copyDocument(String db, String docId, String newDocId) throws CouchDbException, InvalidServerResponseException {
		DocumentInfo doc = null;
		ServerResponse response = null;
		String docContent = getDocumentAsJson(db, docId);
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId) && !isStringEmptyOrNull(newDocId)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(docContent, new TypeReference<Map<String, Object>>() {
				});
				documentMap.put("_id", newDocId);

				response = doRequest(getUrl() + "/" + db, "POST", mapper.writeValueAsString(documentMap), "application/json", false);
				if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
					try {
						Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
						});
						if (responseMap.containsKey("error"))
							throw new CouchDbException("Errored - " + response.data());
						if (responseMap.containsKey("ok") && ((boolean) responseMap.get("ok"))) {
							doc = new DocumentInfo();
							doc.id = (String) responseMap.get("id");
							doc.revision = (String) responseMap.get("rev");
						}

					} catch (IOException e) {
						throw new CouchDbException("Exception - " + e.getMessage());
					}
				} else {
					throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return doc;
	}

	public <T> T getDocument(String db, String docId) throws CouchDbException, InvalidServerResponseException {
		T doc = null;
		String docContent = getDocumentAsJson(db, docId);
		try {
			doc = mapper.readValue(docContent, new TypeReference<T>() {
			});
		} catch (IOException e) {
			throw new CouchDbException("Exception - " + e.getClass().getName() + " - " + e.getMessage());
		}
		return doc;
	}

	public String getDocumentAsJson(String db, String docId, String startKey, String endKey) throws CouchDbException, InvalidServerResponseException {
		String data = null;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId)) {
			String url = getUrl() + String.format("/%s/%s", db, docId);
			try {
				if (!isStringEmptyOrNull(startKey))
					url += "?startkey=" + URLEncoder.encode(startKey, "UTF-8");
				if (!isStringEmptyOrNull(endKey)) {
					if (isStringEmptyOrNull(startKey))
						url += "?";
					else
						url += "&";
					url += "endkey=" + URLEncoder.encode(endKey, "UTF-8");
				}
			} catch (UnsupportedEncodingException e1) {
				throw new CouchDbException(INVALID_ARGUMENTS + " - Invalid keys");
			}
			response = doRequest(url, "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					data = response.data();
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return data;
	}

	public boolean deleteDocument(String db, String docId) throws CouchDbException, InvalidServerResponseException {
		boolean isDeleted = false;
		ServerResponse response = null;
		String docContent = getDocumentAsJson(db, docId);
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(docContent, new TypeReference<Map<String, Object>>() {
				});
				String url = getUrl() + String.format("/%s/%s?rev=%s", db, docId, (String) documentMap.get("_rev"));
				response = doRequest(url, "DELETE", false);
				if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
					try {
						Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
						});
						if (responseMap.containsKey("error"))
							throw new CouchDbException("Errored - " + response.data());
						if (responseMap.containsKey("ok") && ((boolean) responseMap.get("ok")))
							isDeleted = true;
					} catch (IOException e) {
						throw new CouchDbException("Exception - " + e.getMessage());
					}
				} else {
					throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return isDeleted;
	}

	public void deleteDocuments(String db) throws CouchDbException, InvalidServerResponseException {
		ServerResponse response = null;
		Map<String, Object> bulkDeleteMap = new HashMap<String, Object>();
		if (!isStringEmptyOrNull(db)) {
			response = doRequest(getUrl() + "/" + db + "/_all_docs", "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());

					List<Map<String, Object>> rows = (ArrayList<Map<String, Object>>) responseMap.get("rows");
					for (Map<String, Object> row : rows) {
						row.put("_id", row.get("id"));
						row.remove("id");

						if (row.containsKey("value")) {
							Map<String, Object> revision = (Map<String, Object>) row.get("value");
							if (revision.containsKey("rev"))
								row.put("_rev", (String) revision.get("rev"));
						}
						row.remove("value");
						row.put("_deleted", true);
						list.add(row);
					}
					bulkDeleteMap.put("docs", list);

					String url = getUrl() + String.format("/%s/%s", db, "_bulk_docs");
					response = doRequest(url, "POST", mapper.writeValueAsString(bulkDeleteMap), APPLICATION_JSON, false);
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
	}

	public void deleteDocuments(String db, String bulkDeleteCommand) throws CouchDbException {
		if (!isStringEmptyOrNull(db)) {
			doRequest(getUrl() + "/" + db + "/_bulk_docs", "POST", bulkDeleteCommand, APPLICATION_JSON, false);
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
	}

	public boolean existsAttachment(String db, String docId, String attachmentName) throws CouchDbException, InvalidServerResponseException {
		boolean isExist = false;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId) && !isStringEmptyOrNull(attachmentName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, docId), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("_attachments")) {
					Map<String, Object> attachments = (Map<String, Object>) documentMap.get("_attachments");
					if (attachments.containsKey(attachmentName))
						isExist = true;
				}

			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return isExist;
	}

	public void setInlineAttachment(String db, String docId, String attachmentName, String attachmentContentType, byte[] attachmentData) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId) && !isStringEmptyOrNull(attachmentName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, docId), new TypeReference<Map<String, Object>>() {
				});

				if (!documentMap.containsKey("_attachments")) {
					documentMap.put("_attachments", new HashMap<String, Object>());
				}
				;
				Map<String, Object> attachments = (Map<String, Object>) documentMap.get("_attachments");
				if (!attachments.containsKey(attachmentName)) {
					attachments.put(attachmentName, new HashMap<String, Object>());
				}
				Map<String, Object> attachment = (Map<String, Object>) attachments.get(attachmentName);
				attachment.put("content_type", attachmentContentType);
				attachment.put("data", new String(Base64.encodeBase64(attachmentData)));
				
				attachments.put(attachmentName, attachment);
				documentMap.put("_attachments", attachments);

				updateDocument(db, docId, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public ServerResponse getInlineAttachment(String db, String docId, String attachmentName) throws CouchDbException {
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId)) {
			String url = new String();
			try {
				url = getUrl() + String.format("/%s/%s/%s", db, docId, URLEncoder.encode(attachmentName, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			response = doRequest(url, "GET", true);
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return response;
	}

	public void deleteInlineAttachment(String db, String docId, String attachmentName) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId) && !isStringEmptyOrNull(attachmentName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, docId), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("_attachments")) {
					Map<String, Object> attachments = (Map<String, Object>) documentMap.get("_attachments");
					if (attachments.containsKey(attachmentName)) {
						attachments.remove(attachmentName);
					}
					if (attachments.size() == 0)
						documentMap.remove("_attachments");
				}

				updateDocument(db, docId, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public List<InlineAttachmentInfo> getInlineAttachmentInfo(String db, String docId, String attachmentName) throws CouchDbException, InvalidServerResponseException {
		List<InlineAttachmentInfo> list = new ArrayList<InlineAttachmentInfo>();
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(docId)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, docId), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("_attachments")) {
					Map<String, Object> attachments = (Map<String, Object>) documentMap.get("_attachments");
					if (!isStringEmptyOrNull(attachmentName)) {
						for (String key : attachments.keySet()) {
							Map<String, Object> attachment = (Map<String, Object>) attachments.get(key);
							InlineAttachmentInfo attachmentInfo = new InlineAttachmentInfo();
							attachmentInfo.name = key;
							attachmentInfo.contentType = (String) attachment.get("content_type");
							attachmentInfo.length = (String) attachment.get("length");
							list.add(attachmentInfo);
						}
					} else {
						Map<String, Object> attachment = (Map<String, Object>) attachments.get(attachmentName);
						InlineAttachmentInfo attachmentInfo = new InlineAttachmentInfo();
						attachmentInfo.name = attachmentName;
						attachmentInfo.contentType = (String) attachment.get("content_type");
						attachmentInfo.length = (String) attachment.get("length");
						list.add(attachmentInfo);
					}
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return list;
	}

	public List<DocumentInfo> getAllDesignDocuments(String db) throws CouchDbException, InvalidServerResponseException {
		List<DocumentInfo> docList = new ArrayList<DocumentInfo>();
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db)) {
			try {
				String url = getUrl() + String.format("/%s/_all_docs?startkey=%s&endkey=", db, URLEncoder.encode("\"_design\"", "UTF-8"), URLEncoder.encode("\"_design0\"", "UTF-8"));
				response = doRequest(url, "GET", false);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());

					List<Map<String, Object>> rows = (ArrayList<Map<String, Object>>) responseMap.get("rows");
					for (Map<String, Object> row : rows) {
						DocumentInfo doc = new DocumentInfo();
						doc.id = (String) row.get("id");
						if (row.containsKey("value")) {
							Map<String, Object> revision = (Map<String, Object>) row.get("value");
							if (revision.containsKey("rev"))
								doc.revision = (String) revision.get("rev");
						}

						docList.add(doc);
					}
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DATABASE_NAME);
		return docList;
	}

	public DocumentInfo createDesignDocument(String db, String name, String viewName, String map) throws CouchDbException, InvalidServerResponseException {
		DocumentInfo doc = null;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(name) && !isStringEmptyOrNull(viewName) && !isStringEmptyOrNull(map)) {
			Map<String, String> content = new HashMap<String, String>();
			content.put("_id", "_design/" + name);
			content.put("views", viewName);
			content.put("map", map);

			try {
				response = doRequest(getUrl() + "/" + db, "POST", mapper.writeValueAsString(content), "application/json", false);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					if (responseMap.containsKey("ok") && ((boolean) responseMap.get("ok"))) {
						doc = new DocumentInfo();
						doc.id = (String) responseMap.get("id");
						doc.revision = (String) responseMap.get("rev");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);
		return doc;
	}

	public String createView(String db, String viewName, String map, String reduce, String startkey, String endkey) throws CouchDbException, InvalidServerResponseException {
		ServerResponse response = null;
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(map) && !isStringEmptyOrNull(reduce)) {
			String viewdef = "{ \"map\":\"" + map + "\"";
			if (reduce != null)
				viewdef += ",\"reduce\":\"" + reduce + "\"";
			viewdef += "}";

			String url = getUrl() + String.format("/%s/%s", db, viewName);
			try {
				if (!isStringEmptyOrNull(startkey)) {
					url += "?startkey=" + URLEncoder.encode(startkey, "UTF-8");
				}
				if (!isStringEmptyOrNull(endkey)) {
					if (isStringEmptyOrNull(startkey))
						url += "?";
					else
						url += "&";
					url += "endkey=" + URLEncoder.encode(endkey, "UTF-8");
				}

				response = doRequest(url, "POST", viewdef, APPLICATION_JSON, false);
				if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
					result = response.data();
				} else
					throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);

		return result;

	}

	public String createTemporaryView(String db, String map, String reduce, String startkey, String endkey) throws CouchDbException, InvalidServerResponseException {
		String viewName = "_temp_view";
		return createView(db, viewName, map, reduce, startkey, endkey);
	}

	public boolean existsShowFunction(String db, String designDocument, String showFunctionName) throws CouchDbException, InvalidServerResponseException {
		boolean isExist = false;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(showFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("shows")) {
					Map<String, Object> showFunctions = (Map<String, Object>) documentMap.get("shows");
					if (showFunctions.containsKey(showFunctionName))
						isExist = true;
				}

			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return isExist;
	}

	public String getShowFunction(String db, String designDocument, String showFunctionName) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(showFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("shows")) {
					Map<String, Object> showFunctions = (Map<String, Object>) documentMap.get("shows");
					if (showFunctions.containsKey(showFunctionName))
						result = sanitizeOutput((String) showFunctions.get(showFunctionName));
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public void setShowFunction(String db, String designDocument, String showFunctionName, String content) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(showFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (!documentMap.containsKey("shows")) {
					documentMap.put("shows", new HashMap<String, Object>());
				}
				;
				Map<String, Object> showFunctions = (Map<String, Object>) documentMap.get("shows");
				if (!isStringEmptyOrNull(content)) {
					showFunctions.put(showFunctionName, content);
				} else {
					if (showFunctions.containsKey(showFunctionName))
						showFunctions.remove(showFunctionName);
				}
				if (showFunctions.size() == 0)
					documentMap.remove("shows");
				else
					documentMap.put("shows", showFunctions);

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public void deleteShowFunction(String db, String designDocument, String showFunctionName) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(showFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("shows")) {
					Map<String, Object> showFunctions = (Map<String, Object>) documentMap.get("shows");
					if (showFunctions.containsKey(showFunctionName)) {
						showFunctions.remove(showFunctionName);
					}
					if (showFunctions.size() == 0)
						documentMap.remove("shows");
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public boolean existsListFunction(String db, String designDocument, String listFunctionName) throws CouchDbException, InvalidServerResponseException {
		boolean isExist = false;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(listFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("lists")) {
					Map<String, Object> listFunctions = (Map<String, Object>) documentMap.get("lists");
					if (listFunctions.containsKey(listFunctionName))
						isExist = true;
				}

			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return isExist;
	}

	public String getListFunction(String db, String designDocument, String listFunctionName) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(listFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("lists")) {
					Map<String, Object> listFunctions = (Map<String, Object>) documentMap.get("lists");
					if (listFunctions.containsKey(listFunctionName))
						result = sanitizeOutput((String) listFunctions.get(listFunctionName));
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public void setListFunction(String db, String designDocument, String listFunctionName, String content) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(listFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (!documentMap.containsKey("lists")) {
					documentMap.put("lists", new HashMap<String, Object>());
				}
				;
				Map<String, Object> listFunctions = (Map<String, Object>) documentMap.get("lists");
				if (!isStringEmptyOrNull(content)) {
					listFunctions.put(listFunctionName, content);
				} else {
					if (listFunctions.containsKey(listFunctionName))
						listFunctions.remove(listFunctionName);
				}
				if (listFunctions.size() == 0)
					documentMap.remove("lists");
				else
					documentMap.put("lists", listFunctions);

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public void deleteListFunction(String db, String designDocument, String listFunctionName) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(listFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("lists")) {
					Map<String, Object> listFunctions = (Map<String, Object>) documentMap.get("lists");
					if (listFunctions.containsKey(listFunctionName)) {
						listFunctions.remove(listFunctionName);
					}
					if (listFunctions.size() == 0)
						documentMap.remove("lists");
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public <T> List<T> getDesignList(String db, String name, String viewName, List<Map<String, String>> parameters) throws CouchDbException, InvalidServerResponseException {
		List<T> list = new ArrayList<T>();
		ServerResponse response = null;

		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(name) && !isStringEmptyOrNull(viewName)) {
			String param = new String();
			if (parameters.size() > 0) {
				param = "?";
				for (Map<String, String> parameter : parameters) {
					for (String key : parameter.keySet())
						param += String.format("%s=%s&", key, parameter.get(key));
				}
				param = param.substring(0, param.length() - 1);
			}
			String url = getUrl() + String.format("/%s/_design/%s/_list/%s%s", db, name, viewName, param);
			response = doRequest(url, "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> documentMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (documentMap.containsKey("rows")) {
						Map<String, Object> rows = (Map<String, Object>) documentMap.get("rows");
						if (rows.size() > 0) {
							for (String key : rows.keySet()) {
								Map<String, Object> row = (Map<String, Object>) rows.get(key);
								list.add((T) mapper.readValue((String) row.get("value"), new TypeReference<T>() {
								}));
							}
						}
					}
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else
				throw new InvalidServerResponseException("Invalid Server Response! " + response.data());
		} else
			throw new CouchDbException(INVALID_ARGUMENTS);

		return list;
	}

	public ServerResponse getDesignList(String db, String name, String viewName) throws CouchDbException, InvalidServerResponseException {
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(name) && !isStringEmptyOrNull(viewName)) {
			response = doRequest(getUrl() + String.format("/%s/_design/%s/_list/%s", db, name, viewName), "GET", false);
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);

		return response;
	}

	public ServerResponse getDesignList(String db, String name, String listName, String viewName, List<Map<String, String>> parameters) throws CouchDbException, InvalidServerResponseException {
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(name) && !isStringEmptyOrNull(listName) && !isStringEmptyOrNull(viewName)) {
			String param = new String();
			if (parameters.size() > 0) {
				param = "?";
				for (Map<String, String> parameter : parameters) {
					for (String key : parameter.keySet())
						param += String.format("%s=%s&", key, parameter.get(key));
				}
				param = param.substring(0, param.length() - 1);
			}
			response = doRequest(getUrl() + String.format("/%s/_design/%s/_list/%s/%s%s", db, name, listName, viewName, param), "GET", false);
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);

		return response;
	}

	public boolean existsViewFunction(String db, String designDocument, String viewFunctionName) throws CouchDbException, InvalidServerResponseException {
		boolean isExist = false;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(viewFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("views")) {
					Map<String, Object> viewFunctions = (Map<String, Object>) documentMap.get("views");
					if (viewFunctions.containsKey(viewFunctionName))
						isExist = true;
				}

			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return isExist;
	}

	public void setViewFunction(String db, String designDocument, String viewFunctionName, String mapContent, String reduceContent) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(viewFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (!documentMap.containsKey("views")) {
					documentMap.put("views", new HashMap<String, Object>());
				}
				;
				Map<String, Object> viewFunctions = (Map<String, Object>) documentMap.get("views");
				if (!viewFunctions.containsKey(viewFunctionName))
					viewFunctions.put(viewFunctionName, new HashMap<String, Object>());
				Map<String, Object> viewFunction = (Map<String, Object>) viewFunctions.get(viewFunctionName);

				if (!isStringEmptyOrNull(mapContent)) {
					viewFunction.put("map", mapContent);
				} else {
					if (viewFunction.containsKey("map"))
						viewFunction.remove("map");
				}
				if (viewFunctions.size() == 0)
					documentMap.remove("views");
				else {
					viewFunctions.put(viewFunctionName, viewFunction);
					documentMap.put("views", viewFunctions);
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public void deleteViewFunction(String db, String designDocument, String viewFunctionName) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(viewFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("views")) {
					Map<String, Object> listFunctions = (Map<String, Object>) documentMap.get("views");
					if (listFunctions.containsKey(viewFunctionName)) {
						listFunctions.remove(viewFunctionName);
					}
					if (listFunctions.size() == 0)
						documentMap.remove("views");
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public String getMapFunction(String db, String designDocument, String viewFunctionName) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(viewFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("views")) {
					Map<String, Object> viewFunctions = (Map<String, Object>) documentMap.get("views");
					if (viewFunctions.containsKey(viewFunctionName)) {
						Map<String, Object> viewFunction = (Map<String, Object>) viewFunctions.get(viewFunctionName);
						if (viewFunction.containsKey("map"))
							result = sanitizeOutput((String) viewFunction.get("map"));
					}
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public String getReduceFunction(String db, String designDocument, String viewFunctionName) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(viewFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("views")) {
					Map<String, Object> viewFunctions = (Map<String, Object>) documentMap.get("views");
					if (viewFunctions.containsKey(viewFunctionName)) {
						Map<String, Object> viewFunction = (Map<String, Object>) viewFunctions.get(viewFunctionName);
						if (viewFunction.containsKey("reduce"))
							result = sanitizeOutput((String) viewFunction.get("reduce"));
					}
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public <T> List<T> getDesignView(String db, String name, String viewName) throws CouchDbException, InvalidServerResponseException {
		List<T> list = new ArrayList<T>();
		ServerResponse response = null;

		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(name) && !isStringEmptyOrNull(viewName)) {
			String url = getUrl() + String.format("/%s/_design/%s/_view/%s", db, name, viewName);
			response = doRequest(url, "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> documentMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (documentMap.containsKey("rows")) {
						Map<String, Object> rows = (Map<String, Object>) documentMap.get("rows");
						if (rows.size() > 0) {
							for (String key : rows.keySet()) {
								Map<String, Object> row = (Map<String, Object>) rows.get(key);
								list.add((T) mapper.readValue((String) row.get("value"), new TypeReference<T>() {
								}));
							}
						}
					}
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else
				throw new InvalidServerResponseException("Invalid Server Response! " + response.data());
		} else
			throw new CouchDbException(INVALID_ARGUMENTS);

		return list;
	}

	public String getDesignViewAsJson(String db, String name, String viewName) throws CouchDbException, InvalidServerResponseException {
		String data = null;
		ServerResponse response = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(name) && !isStringEmptyOrNull(viewName)) {
			response = doRequest(getUrl() + String.format("/%s/_design/%s/_view/%s", db, name, viewName), "GET", false);
			if (response.contentType().contains("text/plain") || response.contentType().contains("application/json")) {
				try {
					Map<String, Object> responseMap = mapper.readValue(response.data(), new TypeReference<Map<String, Object>>() {
					});
					if (responseMap.containsKey("error"))
						throw new CouchDbException("Errored - " + response.data());
					data = response.data();
				} catch (IOException e) {
					throw new CouchDbException("Exception - " + e.getMessage());
				}
			} else {
				throw new InvalidServerResponseException(INVALID_SERVER_RESPONSE + response.data());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);

		return data;
	}

	public boolean existsValidateFunction(String db, String designDocument) throws CouchDbException, InvalidServerResponseException {
		boolean isExist = false;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("validate_doc_update")) {
					isExist = true;
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return isExist;
	}

	public String getValidateFunction(String db, String designDocument) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("validate_doc_update")) {
					result = sanitizeOutput((String) documentMap.get("validate_doc_update"));
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public void setValidateFunction(String db, String designDocument, String content) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (!isStringEmptyOrNull(content)) {
					documentMap.put("validate_doc_update", content);
				} else {
					if (documentMap.containsKey("validate_doc_update"))
						documentMap.remove("validate_doc_update");
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public void deleteValidateFunction(String db, String designDocument) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("validate_doc_update")) {
					documentMap.remove("validate_doc_update");
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public String getRewriteFunction(String db, String designDocument, int rewriteFunctionNumber) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("rewrites")) {
					List<String> rewriteFunctions = (List<String>) documentMap.get("rewrites");
					if (rewriteFunctions.size() >= rewriteFunctionNumber) {
						result = sanitizeOutput((String) rewriteFunctions.get(rewriteFunctionNumber));
					}
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public void setRewriteFunction(String db, String designDocument, int rewriteFunctionNumber, String content) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (!documentMap.containsKey("rewrites")) {
					documentMap.put("rewrites", new ArrayList<String>());
				}
				;
				List<String> rewriteFunctions = (List<String>) documentMap.get("rewrites");
				if (!isStringEmptyOrNull(content)) {
					if (rewriteFunctions.size() >= rewriteFunctionNumber)
						rewriteFunctions.set(rewriteFunctionNumber, content);
					else
						rewriteFunctions.add(content);
						
				} else {
					if (rewriteFunctions.size() >= rewriteFunctionNumber)
						rewriteFunctions.remove(rewriteFunctionNumber);
				}
				documentMap.put("rewrites", rewriteFunctions);
				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}

	public void deleteRewriteFunction(String db, String designDocument, int rewriteFunctionNumber) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("rewrites")) {
					List<String> rewriteFunctions = (List<String>) documentMap.get("rewrites");
					if (rewriteFunctions.size() >= rewriteFunctionNumber) {
						rewriteFunctions.remove(rewriteFunctionNumber);
					}
					if (rewriteFunctions.size() == 0)
						documentMap.remove("rewrites");
					else
						documentMap.put("rewrites", rewriteFunctions);
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
	}
	
	public boolean existsFilterFunction(String db, String designDocument, String filterFunctionName) throws CouchDbException, InvalidServerResponseException {
		boolean isExist = false;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(filterFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("filters")) {
					Map<String, Object> filterFunctions = (Map<String, Object>) documentMap.get("filters");
					if (filterFunctions.containsKey(filterFunctionName))
						isExist = true;
				}

			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);
		return isExist;
	}

	public String getFilterFunction(String db, String designDocument, String filterFunctionName) throws CouchDbException, InvalidServerResponseException {
		String result = null;
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(filterFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("filters")) {
					Map<String, Object> showFunctions = (Map<String, Object>) documentMap.get("filters");
					if (showFunctions.containsKey(filterFunctionName))
						result = sanitizeOutput((String) showFunctions.get(filterFunctionName));
				}
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_DB_NAME_DOCID);
		return result;
	}

	public void setFilterFunction(String db, String designDocument, String filterFunctionName, String content) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(filterFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (!documentMap.containsKey("filters")) {
					documentMap.put("filters", new HashMap<String, Object>());
				}
				;
				Map<String, Object> filterFunctions = (Map<String, Object>) documentMap.get("filters");
				if (!isStringEmptyOrNull(content)) {
					filterFunctions.put(filterFunctionName, content);
				} else {
					if (filterFunctions.containsKey(filterFunctionName))
						filterFunctions.remove(filterFunctionName);
				}
				if (filterFunctions.size() == 0)
					documentMap.remove("filters");
				else
					documentMap.put("filters", filterFunctions);

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);
	}

	public void deleteFilterFunction(String db, String designDocument, String filterFunctionName) throws CouchDbException, InvalidServerResponseException {
		if (!isStringEmptyOrNull(db) && !isStringEmptyOrNull(designDocument) && !isStringEmptyOrNull(filterFunctionName)) {
			try {
				Map<String, Object> documentMap = mapper.readValue(getDocumentAsJson(db, designDocument), new TypeReference<Map<String, Object>>() {
				});

				if (documentMap.containsKey("filters")) {
					Map<String, Object> filterFunctions = (Map<String, Object>) documentMap.get("filters");
					if (filterFunctions.containsKey(filterFunctionName)) {
						filterFunctions.remove(filterFunctionName);
					}
					if (filterFunctions.size() == 0)
						documentMap.remove("filters");
					else
						documentMap.put("filters", filterFunctions);
				}

				updateDocument(db, designDocument, mapper.writeValueAsString(documentMap));
			} catch (IOException e) {
				throw new CouchDbException("Exception - " + e.getMessage());
			}
		} else
			throw new CouchDbException(CouchDb.INVALID_ARGUMENTS);
	}
}
