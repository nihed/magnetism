/**
 * 
 */
package com.dumbhippo.server;

import javax.naming.NamingException;

import com.dumbhippo.server.client.EjbLink;

/**
 * App that hammers a server with remote session bean calls.
 * Creates test data, so should be run on an empty/trash database.
 * 
 * @author hp
 *
 */
public class TestClient {

	public static void main(String[] args) {
		EjbLink ejb = null;
		try {
			ejb = new EjbLink();
		} catch (NamingException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		TestGlueRemote test = ejb.getTestGlue();
		
		if (test == null)
			System.err.println("TestGlueRemote is null");
		
		test.loadTestData();
	}
}
