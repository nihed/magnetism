/**
 * DumbHippo servlet that does some stuff we don't know yet
 */
package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.NDC;

import com.dumbhippo.server.impl.GlobalSetup;

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
				forward(request, response, "/invite/landing.jsp");
				return;
			} else if (pathinfo.equals("/invite/invite")) {
				forward(request, response, "/invite/invite.jsp");				
				return;
			}	
		}

		super.doGet(request, response);
		return;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NDC.push("[GET/" + request.getRemoteAddr() + "]");
		try {
			doGetInternal(request, response);
		} finally {
			NDC.pop();
		}
	}

	private void doPostInternal(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		String path;
		String pathinfo;
		path = request.getServletPath();
		pathinfo = request.getPathInfo();
		logger.debug("Request path=" + path + " pathinfo=" + request.getPathInfo() + " contextpath="
				+ request.getContextPath());

		if (path.equals("/web")) {
			if (pathinfo.equals("/invite/submit")) {
				forward(request, response, "/invite/submit.jsp");
				return;
			}
		}

		super.doPost(request, response);
		return;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NDC.push("[POST/" + request.getRemoteAddr() + "]");
		try {
			doPostInternal(request, response);
		} finally {
			NDC.pop();
		}
	}

	@Override
	public void init() throws ServletException {
		super.init();
		GlobalSetup.initialize();	
	}
}
