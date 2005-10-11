package com.dumbhippo.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

public class TestXmlRpcClient {

	public static void main(String[] args) {
		XmlRpcClient xmlrpc = null;
		try {
			xmlrpc = new XmlRpcClient("http://localhost:8080/dumbhippo/xmlrpc");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		Vector params = new Vector();
		// params.addElement ("some parameter");
		
		Object result = null;
		try {
			result = xmlrpc.execute ("dumbhippo.getStuff", params);
		} catch (XmlRpcException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Got result: '" + result + "'");
	}
}
