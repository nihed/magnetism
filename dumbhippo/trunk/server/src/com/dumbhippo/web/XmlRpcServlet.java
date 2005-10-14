package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.web.EjbLink.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class XmlRpcServlet extends HttpServlet {

	static Log logger = LogFactory.getLog(XmlRpcServlet.class);
	
	private static final long serialVersionUID = 0L;

	private AjaxGlueXmlRpc glue;

	private XmlRpcServer xmlrpc;

	private void setup(HttpServletRequest request) throws ServletException {
		if (xmlrpc != null)
			return;

		EjbLink ejb = new EjbLink();

		try {
			ejb.attemptLogin(request);
		} catch (BadTastingException e) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI

			e.printStackTrace();
			throw new ServletException("Authorization failed (bad cookie), please log in again", e);
		} catch (NotLoggedInException e) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI
			
			e.printStackTrace();
			throw new ServletException("Authorization failed (not logged in), please log in again", e);
		}

		glue = ejb.getEjb(AjaxGlueXmlRpc.class);
				
		// init "xmlrpc" last since it indicates whether we are done setting up
		xmlrpc = new XmlRpcServer();
		
		// This is only safe because EjbLink.nameLookup() creates a proxy with ONLY 
		// our requested interface, so we aren't exporting random crap remotely.
		xmlrpc.addHandler("dumbhippo", glue);
	}

	private void logRequest(HttpServletRequest request, String type) {
		logger.info(type + " uri=" + request.getRequestURI());
		Enumeration names = request.getAttributeNames(); 
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			
			logger.info("attr " + name + " = " + request.getAttribute(name));
		}
		
		names = request.getParameterNames();		
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String[] values = request.getParameterValues(name);
			StringBuilder builder = new StringBuilder();
			for (String v : values) {
				builder.append("'" + v + "',");
			}
			builder.deleteCharAt(builder.length() - 1); // drop comma
			
			logger.info("param " + name + " = " + builder.toString());
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logRequest(request, "POST");
		
		setup(request);

		if (request.getRequestURI().startsWith("/xmlrpc/")) {
			byte[] result = xmlrpc.execute(request.getInputStream());
			response.setContentType("text/xml");
			response.setContentLength(result.length);
			OutputStream out = response.getOutputStream();
			out.write(result);
			out.flush();
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logRequest(request, "GET");
		
		try {
			setup(request);
		} catch (ServletException e) {
			logger.error(e);
			throw e;
		}
		
		if (request.getRequestURI().equals("/xml/friendcompletions")) {
			
			String entryContents = request.getParameter("entryContents");
			
			response.setContentType("text/xml");
			OutputStream out = response.getOutputStream();
			
			out.write("<ul>\n".getBytes());
			List<String> completions = glue.getFriendCompletions(entryContents);
			for (String c : completions) {
				out.write("<li>".getBytes());
				out.write(c.getBytes());
				out.write("</li>\n".getBytes());
			}
			out.write("</ul>".getBytes());
			out.flush();
		}
	}
}
