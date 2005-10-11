package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlue;
import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.web.LoginCookie.BadTastingException;
import com.dumbhippo.web.LoginCookie.NotLoggedInException;

public class XmlRpcServlet extends HttpServlet {

	static Log logger = LogFactory.getLog(XmlRpcServlet.class);
	
	private static final long serialVersionUID = 0L;

	private AjaxGlue glue;

	private AjaxGlueXmlRpc glueProxy;

	private XmlRpcServer xmlrpc;

	private void setup(HttpServletRequest request) throws ServletException {
		if (xmlrpc != null)
			return;

		AccountSystem accountSystem;
		
		try {
			InitialContext initialContext = new InitialContext();
			glue = (AjaxGlue) initialContext.lookup(AjaxGlue.class.getCanonicalName());
			accountSystem = (AccountSystem) initialContext.lookup(AccountSystem.class.getCanonicalName());
		} catch (NamingException e) {
			e.printStackTrace();
			throw new ServletException("Failed to lookup AjaxGlue", e);
		}
		
		if (glue == null || accountSystem == null) {
			throw new IllegalStateException("Failed to get session beans from JNDI");
		}
		
		try {
			HippoAccount account = LoginCookie.attemptLogin(accountSystem, request);
			glue.init(account.getOwner().getId());
		} catch (BadTastingException e) {
			
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI
			
			e.printStackTrace();
			throw new ServletException("Authorization failed, please log in again", e);
		} catch (NotLoggedInException e) {
			// In an HTML servlet, we would redirect to a login page; but in this
			// servlet we can't do much, we have no UI
			
			e.printStackTrace();
			throw new ServletException("Authorization failed, please log in again", e);
		}
		
		// glueProxy ONLY Implements AjaxGlueXmlRpc, not anything in AjaxGlue
		glueProxy = (AjaxGlueXmlRpc) InterfaceFilterProxyFactory.newProxyInstance(glue,
				new Class[] { AjaxGlueXmlRpc.class });

		if (glueProxy instanceof AjaxGlue)
			throw new IllegalStateException("Bug! glueProxy implements AjaxGlue");

		// init "xmlrpc" last since it indicates whether we are done setting up
		xmlrpc = new XmlRpcServer();
		xmlrpc.addHandler("dumbhippo", glueProxy);
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
			
			logger.info("param " + name + " = " + request.getAttribute(name));
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logRequest(request, "POST");
		
		setup(request);

		byte[] result = xmlrpc.execute(request.getInputStream());
		response.setContentType("text/xml");
		response.setContentLength(result.length);
		OutputStream out = response.getOutputStream();
		out.write(result);
		out.flush();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logRequest(request, "GET");
		
		try {
			setup(request);
		} catch (ServletException e) {
			// FIXME ignoring for now for testing
		}
		
		if (request.getRequestURI().equals("/xml/people")) {
			
			response.setContentType("text/xml");
			OutputStream out = response.getOutputStream();
			
			out.write("<ul>\n".getBytes());
			List<String> completions = glue.getFriendCompletions("FIXME - contents of entry box");
			for (String c : completions) {
				out.write("<li>".getBytes());
				out.write(c.getBytes());
				out.write("</li>\n".getBytes());
			}
			out.write("<li>Bogus Extra Person Not In Database</li>".getBytes());
			out.write("<li>Yet Another Bogus Extra Person</li>".getBytes());
			out.write("</ul>".getBytes());
			out.flush();
		}
	}
}
