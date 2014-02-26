package com.skitsanos.api;

import java.io.IOException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class ConnectionBase {

	private String username = "";
	private String password = "";
	private boolean useSsl = false;
	private String host = "localhost";
	private int port = 5984;

	public String host() {
		return host;
	}

	public void host(String host) {
		this.host = host;
	}

	public int port() {
		return port;
	}

	public void port(int port) {
		this.port = port;
	}

	public boolean useSsl() {
		return useSsl;
	}

	public void useSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public String username() {
		return username;
	}

	public void username(String username) {
		this.username = username;
	}

	public String password() {
		return password;
	}

	public void password(String password) {
		this.password = password;
	}

	protected String getUrl() {
		String url = "http://";
		if (useSsl)
			url = "https://";

		if (host.contains("cloudant.com") && useSsl) {
			url += host;
		} else {
			url += host;
			if (port > 0)
				url += ":" + port;
		}

		return url;
	}

	private void setAuthorizationHeader(HttpRequestBase request) {
		if ((username.length() > 0) && (password.length() > 0)) {
			request.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", username, password).getBytes())));
			// + Base64.encodeBase64String(String.format("%s:%s", username,
			// password).getBytes()));
		}
	}

	private ServerResponse getServerResponse(HttpResponse httpResponse, boolean isBinaryResult) {
		ServerResponse response = new ServerResponse();
		response.contentType(httpResponse.getEntity().getContentType().getValue());
		byte[] data = null;
		try {
			if (!isBinaryResult) {
				String content = EntityUtils.toString(httpResponse.getEntity());
				//System.out.println(content);
				response.data(content);
			} else {
				data = EntityUtils.toByteArray(httpResponse.getEntity());
				response.data(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	protected ServerResponse doRequest(String url, String method, Boolean isBinaryResult) {
		return doRequest(url, method, "", "", isBinaryResult);
	}

	protected ServerResponse doRequest(String url, String method, String data, String contentType, Boolean isBinaryResult) {

		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse = null;
		ServerResponse response = null;
		long startTime = 0L;
		long execTime = 0L;
		try {

			if (method.equalsIgnoreCase("GET")) {
				HttpGet request = new HttpGet(url);
				setAuthorizationHeader(request);
				startTime = System.currentTimeMillis();
				httpResponse = httpClient.execute(request);
			} else if (method.equalsIgnoreCase("POST")) {
				HttpPost request = new HttpPost(url);
				setAuthorizationHeader(request);
				request.addHeader("Content-Type", contentType);
				StringEntity postData = new StringEntity(data);
				request.setEntity(postData);
				startTime = System.currentTimeMillis();
				httpResponse = httpClient.execute(request);
			} else if (method.equalsIgnoreCase("DELETE")) {
				HttpDelete request = new HttpDelete(url);
				setAuthorizationHeader(request);
				request.addHeader("Content-Type", contentType);
				startTime = System.currentTimeMillis();
				httpResponse = httpClient.execute(request);
			} else {
				HttpPut request = new HttpPut(url);				
				setAuthorizationHeader(request);
				request.addHeader("Content-Type", contentType);
				startTime = System.currentTimeMillis();
				httpResponse = httpClient.execute(request);
			}
			execTime = System.currentTimeMillis() - startTime;
			response = getServerResponse(httpResponse, isBinaryResult);
			response.execTime(execTime);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

		return response;

	}
	
	protected String sanitizeOutput(String content)
	{
		String result = content;

		result = result.trim();

		result = result.replace("\\r\\n", "THIS_IS_NOT_A_RETURN_NEWLINE");
		result = result.replace("\r\n", System.lineSeparator());

		result = result.replace("\\n", "THIS_IS_NOT_A_NEWLINE");
		result = result.replace("\n", System.lineSeparator());
		result = result.replace("THIS_IS_NOT_A_NEWLINE", "\n");

		result = result.replace("THIS_IS_NOT_A_RETURN_NEWLINE", "\r\n");

		result = result.replace("\\t", "THIS_IS_NOT_A_TAB");
		result = result.replace("\t", "\t");
		result = result.replace("THIS_IS_NOT_A_TAB", "\t");

		result = result.replace("\\\"", "\"");
		result = result.replace("\\\\", "\\");

		return result;
	}
}
