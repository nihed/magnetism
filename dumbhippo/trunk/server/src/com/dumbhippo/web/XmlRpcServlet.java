package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AjaxGlue;
import com.dumbhippo.server.AjaxGlueXmlRpc;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class XmlRpcServlet extends HttpServlet {

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

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {

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
		response.setContentType("text/html");
		OutputStream out = response.getOutputStream();
		out.write("<h1>XML-RPC Servlet</h1><p>(insert docs for XML-RPC stuff here?)</p>".getBytes());
		out.flush();
	}
}
