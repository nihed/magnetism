package com.dumbhippo.jive;

import javax.naming.NamingException;

import org.jivesoftware.util.Log;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.client.EjbLink;

public class Server {

	private static Server instance;
	
	private static synchronized Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}
	
	public static MessengerGlueRemote getMessengerGlue() {
		return getInstance().ejb.getMessengerGlue();
	}
	
	private EjbLink ejb;
	
	private Server() {
		try {
			ejb = new EjbLink();
		} catch (NamingException e) {
			Log.error("Failure connecting to server");
			Log.error(e);
			e.printStackTrace();
			throw new Error("Could not connect to server", e);
		}
	}
}
