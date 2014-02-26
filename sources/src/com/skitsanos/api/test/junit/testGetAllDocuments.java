package com.skitsanos.api.test.junit;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.skitsanos.api.CouchDb;
import com.skitsanos.api.DocumentInfo;
import com.skitsanos.api.exceptions.CouchDbException;
import com.skitsanos.api.exceptions.InvalidServerResponseException;

public class testGetAllDocuments {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void testgetAllDocuments() {
		CouchDb couchdb = new CouchDb(TestConf.hostName, TestConf.port, TestConf.userName, TestConf.password);
		String version = null;
		List<DocumentInfo> docList = new ArrayList<DocumentInfo>();
		try {
			docList = couchdb.getAllDocuments(TestConf.dbName);
		} catch (InvalidServerResponseException | CouchDbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (docList.size() == 0)
			fail("Not yet implemented");
		else {
			System.out.println("Doc Length - " + docList.size());
			for (DocumentInfo doc : docList) {
				System.out.println ("id - " + doc.id + ", rev - " + doc.revision);
			}
		}
	}

}
