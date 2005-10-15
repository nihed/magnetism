package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.web.EjbLink.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class XmlRpcServlet extends HttpServlet {

	private static final Log logger = GlobalSetup.getLog(XmlRpcServlet.class);
	
	private static final long serialVersionUID = 0L;

	private static final String XMLRPC_KEY = "org.dumbhippo.web.XmlRpcServlet.XmlRpcServer";
	
	private XmlRpcServer getSessionXmlRpc(HttpServletRequest request) throws ServletException {
		HttpSession session = request.getSession();
		XmlRpcServer xmlrpc;
	
		synchronized (session) {
			xmlrpc = (XmlRpcServer) session.getAttribute(XMLRPC_KEY);
			if (xmlrpc == null) {
				xmlrpc = new XmlRpcServer();
				
				// Java thread locks are recursive so this is OK...
				AjaxGlueXmlRpc glue = getSessionGlue(request);		
				
				// glue is a proxy that only exports the one interface, 
				// so safe to export it all
				xmlrpc.addHandler("dumbhippo", glue);
				
				session.setAttribute(XMLRPC_KEY, xmlrpc);
			}
		}
		
		return xmlrpc;
	}
	
	private AjaxGlueXmlRpc getSessionGlue(HttpServletRequest request) throws ServletException {

		EjbLink ejb = EjbLink.getForSession(request.getSession());

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

		return ejb.getEjb(AjaxGlueXmlRpc.class);
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
		
		if (!request.getRequestURI().startsWith("/xmlrpc/")) {
			return;
		}
		
		XmlRpcServer xmlrpc = getSessionXmlRpc(request);

		// no idea if xmlrpc is in fact thread-safe, but 
		// let's serialize all our uses of it... it's per-session
		// so should be no thread contention kind of issues
		synchronized (xmlrpc) {
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

		if (!request.getRequestURI().startsWith("/xml/")) {
			return;
		}
	
		AjaxGlueXmlRpc glue;
		
		try {
			glue = getSessionGlue(request);
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
