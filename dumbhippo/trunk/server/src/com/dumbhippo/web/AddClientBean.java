package com.dumbhippo.web;

import javax.annotation.EJB;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.EJBUtil;
import com.dumbhippo.server.TestGlue;


public class AddClientBean {
	
	static private final Log logger = GlobalSetup.getLog(AddClientBean.class);
	
	private String email;
	
	private String goBackTo;
	
	private TestGlue testGlue;
	
	public AddClientBean() {
		testGlue = WebEJBUtil.defaultLookup(TestGlue.class);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getGoBackTo() {
		return goBackTo;
	}

	public void setGoBackTo(String goBackTo) {
		logger.debug("goBackTo = " + goBackTo);
		this.goBackTo = goBackTo;
	}

	// FIXME this should go away
	public static LoginCookie addNewClientForEmail(String email, HttpServletRequest request, HttpServletResponse response) {
		TestGlue testGlue = WebEJBUtil.defaultLookup(TestGlue.class);
		HippoAccount account = testGlue.findOrCreateAccountFromEmail(email);
		String authKey = testGlue.authorizeNewClient(account.getId(), SigninBean.computeClientIdentifier(request));
		LoginCookie loginCookie = new LoginCookie(account.getOwner().getId(), authKey);
		response.addCookie(loginCookie.getCookie());
		HttpSession sess = request.getSession(false);
		if (sess != null)
			sess.invalidate();
		return loginCookie;
	}	
	
	public String doAddClient() {

		logger.debug("doAddClient()");
		
		if (email == null) {
			logger.debug("no email filled in");
			return null;
		}
		
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
				
		addNewClientForEmail(email, req, response);

		if (goBackTo != null) {
			logger.debug("sending back to " + goBackTo);
			return goBackTo;
		} else {
			logger.debug("sending to main");
			return "main";
		}
	}
}
