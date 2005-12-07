package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HumanVisibleException;

public class RedirectServlet extends AbstractServlet {
	
	private static final long serialVersionUID = 1L;

	private void writeHtmlHeaderBoilerplate(OutputStream out) throws IOException {
		out.write(("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">"
				+ "<html>"
				+ "<head>"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
				+ "</head><body>").getBytes());		
	}
	
	private boolean tryRedirectRequests(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException {
		if (!request.getRequestURI().equals("/redirect")) {
			return false;
		}
		
		String url = request.getParameter("url");
		String postId = request.getParameter("postId");
		String inviteKey = request.getParameter("inviteKey");
		
		if (postId == null || url == null) {
			// print a little message about this page
			response.setContentType("text/plain");
			OutputStream out = response.getOutputStream();
			out.write("This page forwards you to a shared link, and tells the friend who shared the link that you're going to look at it.\n".getBytes());
			out.write("It isn't doing that right now though, because it didn't receive the right parameters.\n".getBytes());
			out.flush();
		} else {
			HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);
			User user;
			try {
				user = doLogin(request);
			} catch (HttpException e) {
				user = null; // not fatal as it usually is
			}
			try { 
				glue.handleRedirect(user, url, postId, inviteKey);
				
				// If "url" were on our own site, we would need to call encodeRedirectURL to be sure it had the jsessionid in it
				response.sendRedirect(url);
				response.setContentType("text/html");
				OutputStream out = response.getOutputStream();
				writeHtmlHeaderBoilerplate(out);
				String escapedUrl = XmlBuilder.escape(url);
				out.write(("<p>Go to <a href=\"" + escapedUrl + "\">" + escapedUrl + "</a></p>\n").getBytes());
				out.write("</body>\n".getBytes());
				out.flush();
			} catch (HumanVisibleException e) {
				response.setContentType("text/html");
				OutputStream out = response.getOutputStream();
				writeHtmlHeaderBoilerplate(out);
				
				out.write("<p>Oops! Could not send you to your link automatically.</p>".getBytes());
				out.write(("<p><i>" + e.getHtmlMessage() + "</i></p>").getBytes());
				
				String escapedUrl = XmlBuilder.escape(url);
				out.write(("<p style=\"font-size: larger;\">Try <a href=\"" + escapedUrl + "\">clicking here</a></p>\n").getBytes());
				
				out.write("</body>\n".getBytes());
				out.flush();
			}
		}
		
		return true;
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException, HumanVisibleException {
		
		setNoCache(response);
		
		if (!tryRedirectRequests(request, response))
			throw new HttpException(HttpResponseCode.NOT_FOUND, "unknown redirect");
	}
	
	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, HumanVisibleException {
		setNoCache(response);
		
		if (!tryRedirectRequests(request, response))
			throw new HttpException(HttpResponseCode.NOT_FOUND, "unknown redirect");
	}
}
