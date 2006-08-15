package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.web.WebEJBUtil;

public class RedirectServlet extends AbstractServlet {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(RedirectServlet.class);
	
	private static final long serialVersionUID = 1L;

	private void writeHtmlHeaderBoilerplate(OutputStream out) throws IOException {
		out.write(("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">"
				+ "<html>"
				+ "<head>"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
				+ "</head><body>").getBytes());		
	}

	private String getUrlFromInviteKey(String inviteKey, String origUrl, String postId) throws HumanVisibleException {
		StringBuilder sb = new StringBuilder();
		sb.append("/verify?authKey=");
		sb.append(StringUtils.urlEncode(inviteKey));
		sb.append("&url=");
		sb.append(StringUtils.urlEncode(origUrl));
		sb.append("&viewedPostId=");
		sb.append(StringUtils.urlEncode(postId));
		return sb.toString();
	}
	
	private boolean tryRedirectRequests(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException, HumanVisibleException {
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
			return true;
		} 
		
		try {
			if (inviteKey != null) {
				// change the url to be the new verify url if 
				// we have an invite key. VerifyServlet will do the 
				// postViewedBy
				url = getUrlFromInviteKey(inviteKey, url, postId);
				// this is a redirect to ourselves so we need this
				// to get the jsessionid, at least in theory
				url = response.encodeRedirectURL(url);
			} else {
				User user = getUser(request);
				if (user != null) {
					PostingBoard postingBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
					postingBoard.postViewedBy(postId, user);
				}
			}
			
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
			
			// rewrite to be mildly more helpful
			
			HumanVisibleException ePrime = new HumanVisibleException(
			"<p>Could not send you to your link automatically.</p>" +
			"<p><i>" + e.getHtmlMessage() + "</i></p>", true);
			
			String escapedUrl = XmlBuilder.escape(url);
			ePrime.setHtmlSuggestion("<p style=\"font-size: larger;\">Try <a href=\"" + escapedUrl + "\">clicking here</a></p>\n");
			
			throw ePrime;
		}
		
		return true;
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException, HumanVisibleException {
		
		setNoCache(response);
		
		if (!tryRedirectRequests(request, response))
			throw new HttpException(HttpResponseCode.NOT_FOUND, "unknown redirect");
		
		return null;
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, HumanVisibleException {
		setNoCache(response);
		
		if (!tryRedirectRequests(request, response))
			throw new HttpException(HttpResponseCode.NOT_FOUND, "unknown redirect");
		
		return null;
	}

	@Override
	protected boolean requiresTransaction() {
		return false;
	}
}
