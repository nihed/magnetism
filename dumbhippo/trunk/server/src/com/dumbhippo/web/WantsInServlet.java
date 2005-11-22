package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.WantsInSystem;

public class WantsInServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(WantsInServlet.class);

	private static final long serialVersionUID = 1L;

	private WantsInSystem wantsInSystem;
	
	@Override
	public void init() {
		wantsInSystem = WebEJBUtil.defaultLookup(WantsInSystem.class);
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, ErrorPageException {
		String address = request.getParameter("address");
		if (address != null)
			address = address.trim();
		if (address == null || address.length() == 0 || address.indexOf('@') < 1 || address.equals("let@me.in.please")) {
			throw new ErrorPageException("You have to put in an email address");
		}
		
		wantsInSystem.addWantsIn(address);
		
		response.setContentType("text/html");
		OutputStream out = response.getOutputStream();
		out.write(("<head><title>Saved your address</title></head>\n"
				+ " <body><p>We saved your address; we'll let you know when we have room for more.</p><p>Thanks!</p></body>\n").getBytes());
		out.flush();
	}
}
