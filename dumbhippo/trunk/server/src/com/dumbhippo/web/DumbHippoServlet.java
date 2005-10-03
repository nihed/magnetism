/**
 * DumbHippo servlet that does some stuff we don't know yet
 */
package com.dumbhippo.web;

import java.io.IOException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.NDC;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.GlobalSetup;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderBean;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemBean;

/**
 * DumbHippo servlet that does some stuff we don't know yet
 * 
 * @author hp
 * 
 */
public class DumbHippoServlet extends HttpServlet {
	private static final long serialVersionUID = 0;

	static Log logger = LogFactory.getLog(DumbHippoServlet.class);

	private void forward(HttpServletRequest request, HttpServletResponse response, String forwardTo)
			throws ServletException, IOException {
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(forwardTo);
		if (dispatcher == null)
			super.doGet(request, response);
		else {
			dispatcher.forward(request, response);
		}
	}

	private void doInviteLanding(HttpServletRequest request, HttpServletResponse response, String authKey)
			throws ServletException, IOException {
		InvitationSystemBean invitesystem = new InvitationSystemBean();
		Invitation i = invitesystem.lookupInvitationByKey(authKey);
		request.setAttribute("invitation", i);
		forward(request, response, "/invite/landing.jsp");
	}

	private void doInvite(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		forward(request, response, "/invite/invite.jsp");
	}

	private void doGetInternal(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String path;
		String pathinfo;
		path = request.getServletPath();
		pathinfo = request.getPathInfo();
		logger.debug("Request path=" + path + " pathinfo=" + request.getPathInfo() + " contextpath="
				+ request.getContextPath());

		if (path.equals("/web")) {
			if (pathinfo.equals("/invite/landing")) {
				String authKey = request.getParameter("auth");
				if (authKey != null) {
					doInviteLanding(request, response, authKey);
					return;
				} else {
					logger.info("No auth parameter given!");
				}
			} else if (pathinfo.equals("/invite/invite")) {
				doInvite(request, response);
				return;
			}
		}

		super.doGet(request, response);
		return;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NDC.push("[GET/" + request.getRemoteAddr() + "]");
		SessionWrapper wrapper = Storage.getGlobalPerThreadSession();
		try {
			wrapper.beginTransaction();
			doGetInternal(request, response);
			wrapper.commitTransaction();
		} finally {
			wrapper.closeSession();
			NDC.pop();
		}
	}

	private void doActionInvite(HttpServletRequest request, HttpServletResponse response, String emailAddress)
			throws ServletException, IOException {
		IdentitySpider spider = new IdentitySpiderBean();
		InvitationSystem invitesystem = new InvitationSystemBean();
		if (emailAddress == null)
			return;
		try {
			@SuppressWarnings("unused")
			InternetAddress emailAddr = new InternetAddress(emailAddress);
		} catch (AddressException e) {
			throw new ServletException("Malformed email address");
		}
		EmailResource res = spider.getEmail(emailAddress);
		// FIXME we should get the person from the auth data
		Person inviter = spider.getTheMan();
		Invitation invite = invitesystem.createGetInvitation(inviter, res);
		logger.debug("Created invitation with auth " + invite.getAuthKey());
		request.setAttribute("invitation", invite);
		forward(request, response, "/invite/submitted.jsp");
	}

	private void doPostInternal(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String path;
		String pathinfo;
		path = request.getServletPath();
		pathinfo = request.getPathInfo();
		logger.debug("Request path=" + path + " pathinfo=" + request.getPathInfo() + " contextpath="
				+ request.getContextPath());

		if (path.equals("/actions")) {
			if (pathinfo.equals("/invite")) {
				String email = request.getParameter("emailaddr");
				if (email != null) {
					doActionInvite(request, response, email);
					return;
				} else {
					logger.info("No emailaddr parameter given!");
				}
			}
		}

		super.doPost(request, response);
		return;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NDC.push("[POST/" + request.getRemoteAddr() + "]");
		SessionWrapper wrapper = Storage.getGlobalPerThreadSession();
		try {
			wrapper.beginTransaction();
			doPostInternal(request, response);
			wrapper.commitTransaction();
		} finally {
			wrapper.closeSession();
			NDC.pop();
		}
	}

	@Override
	public void init() throws ServletException {
		super.init();
		GlobalSetup.initialize();	
	}
}
