/**
 * DumbHippo servlet that does some stuff we don't know yet
 */
package com.dumbhippo.server;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.NDC;

import com.dumbhippo.persistence.Storage;

/**
 * DumbHippo servlet that does some stuff we don't know yet
 * 
 * @author hp
 * 
 */
public class DumbHippoServlet extends HttpServlet {
	private static final long serialVersionUID = 0;

	static Log logger = LogFactory.getLog(DumbHippoServlet.class);
	
	private void forward(HttpServletRequest request, HttpServletResponse response, String forwardTo) throws ServletException, IOException {
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(forwardTo);
        if (dispatcher == null)
            super.doGet(request, response);
        else {
            dispatcher.forward(request, response);
        }		
	}

	private void doGetInvite(HttpServletRequest request, HttpServletResponse response, String authKey)
			throws ServletException, IOException {
		InvitationSystemBean invitesystem = new InvitationSystemBean();
		Invitation i = invitesystem.lookupInvitationByKey(authKey);
		request.setAttribute("invitation", i);
		forward(request, response, "/invite.jsp");
	}

	private void doGetInternal(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String path;
		path = request.getServletPath();
		logger.info("Servlet path requested: " + path);

		if (path.equals("/invite")) {
			String authKey = request.getParameter("auth");
			if (authKey != null) {
				doGetInvite(request, response, authKey);
				return;
			}
		}

		super.doGet(request, response);
		return;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NDC.push("[GET/" + request.getRemoteAddr() + "]");
		try {
			doGetInternal(request, response);
		} finally {
			NDC.pop();
		}
	}

	@Override
	public void init() throws ServletException {
		super.init();
		GlobalSetup.initialize();
		logger.info("Booting");
		// FIXME shouldn't be hardcoded		
		Storage.initGlobalInstance("/tmp/dumbhippo-storage");		
	}
}
