package com.skitsanos.api.test.junit;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.skitsanos.api.CouchDb;
import com.skitsanos.api.exceptions.CouchDbException;
import com.skitsanos.api.exceptions.InvalidServerResponseException;

public class testVersion {

	@Test
	public final void testVersion() {
		CouchDb couchdb = new CouchDb(TestConf.hostName, TestConf.port, TestConf.userName, TestConf.password);
		String version = null;
		try {
			version = couchdb.version();
		} catch (InvalidServerResponseException | CouchDbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (version == null)
			fail("Not yet implemented");
		else 
			System.out.print("Version - " + version);
	}

}
