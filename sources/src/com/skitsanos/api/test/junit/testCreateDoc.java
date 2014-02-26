package com.skitsanos.api.test.junit;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.skitsanos.api.CouchDb;
import com.skitsanos.api.DocumentInfo;
import com.skitsanos.api.exceptions.CouchDbException;
import com.skitsanos.api.exceptions.InvalidServerResponseException;

public class testCreateDoc {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void testString() {
		CouchDb couchdb = new CouchDb(TestConf.hostName, TestConf.port, TestConf.userName, TestConf.password);
		DocumentInfo doc = null;
		try {
			doc = couchdb.createDocument(TestConf.dbName, "{\"error\":\"bad_request\",\"reason\":\"invalid_json\"}");
		} catch (InvalidServerResponseException | CouchDbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		if (doc == null)
			fail("Errored");
		else {
			System.out.println("Doc id - " + doc.id + ", rev - " + doc.revision);			
		}
	}
	
	@Test
	public final void testObject() {
		CouchDb couchdb = new CouchDb(TestConf.hostName, TestConf.port, TestConf.userName, TestConf.password);
		DocumentInfo doc = null;
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("data1", "value1");			
			doc = couchdb.createDocument(TestConf.dbName, map);
		} catch (InvalidServerResponseException | CouchDbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		if (doc == null)
			fail("Errored");
		else {
			System.out.println("Doc id - " + doc.id + ", rev - " + doc.revision);			
		}
	}

}
