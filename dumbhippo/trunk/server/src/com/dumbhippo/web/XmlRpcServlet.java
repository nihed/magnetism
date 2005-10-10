package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcServer;

public class XmlRpcServlet extends HttpServlet {

	private static final long serialVersionUID = 0L;

	class ExampleHandler {
		public String getStuff() {
			return "This is some stuff!";
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		XmlRpcServer xmlrpc = new XmlRpcServer();
		
		xmlrpc.addHandler ("examples", new ExampleHandler ());

		byte[] result = xmlrpc.execute (request.getInputStream ());
		response.setContentType ("text/xml");
		response.setContentLength (result.length);
		OutputStream out = response.getOutputStream();
		out.write (result);
		out.flush ();
	}	
}
