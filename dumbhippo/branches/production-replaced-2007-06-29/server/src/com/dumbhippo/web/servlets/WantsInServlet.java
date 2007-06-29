package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.web.WebEJBUtil;

public class WantsInServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(WantsInServlet.class);

	private static final long serialVersionUID = 1L;

	private WantsInSystem wantsInSystem;
	
	@Override
	public void init() {
		wantsInSystem = WebEJBUtil.defaultLookup(WantsInSystem.class);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, HumanVisibleException {
		String address = request.getParameter("address");
		if (address != null)
			address = address.trim();
		if (address == null || address.length() == 0 || address.indexOf('@') < 1 || address.endsWith("example.com")) {
			throw new HumanVisibleException("You have to put in an email address").setHtmlSuggestion("<a href=\"/\">Try again</a>");
		}
		
		try {
			wantsInSystem.addWantsIn(address);
		} catch (ValidationException e) {
			throw new HumanVisibleException("Something was wrong with that email address (" + e.getMessage() + ")").setHtmlSuggestion("<a href=\"/\">Try again</a>");
		}
		
		response.setContentType("text/html");
		OutputStream out = response.getOutputStream();
		out.write(("<head><title>Saved your address</title></head>\n"
				+ " <body><p>We saved your address; we'll let you know when we have room for more.</p><p>Thanks!</p></body>\n").getBytes());
		out.flush();
		
		return null;
	}

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}
}
