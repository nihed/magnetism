package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.XmlRpcMethods;

public class XmlRpcServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException, HumanVisibleException {
		
		setNoCache(response);
		
		if (request.getRequestURI().startsWith("/xmlrpc/")) {
			XmlRpcServer xmlrpc = new XmlRpcServer();
			// Java thread locks are recursive so this is OK...
			XmlRpcMethods glue = WebEJBUtil.defaultLookup(XmlRpcMethods.class);

			// glue is a proxy that only exports the one interface,
			// so safe to export it all
			xmlrpc.addHandler("dumbhippo", glue);
			byte[] result = xmlrpc.execute(request.getInputStream());
			response.setContentType("text/xml");
			response.setContentLength(result.length);
			OutputStream out = response.getOutputStream();
			out.write(result);
			out.flush();
		} else {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "no such method");
		}
	}
}
