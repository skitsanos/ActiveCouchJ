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

public class testCountDocs {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void test() {
		CouchDb couchdb = new CouchDb(TestConf.hostName, TestConf.port, TestConf.userName, TestConf.password);
		int count = -1;		
		try {
			count = couchdb.countDocuments(TestConf.dbName);
		} catch (InvalidServerResponseException | CouchDbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		if (count == -1)
			fail("Errored");
		else {
			System.out.println("Doc Count - " + count);
			
		}
	}

}
