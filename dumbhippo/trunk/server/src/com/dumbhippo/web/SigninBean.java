package com.dumbhippo.web;

import javax.ejb.EJBException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.web.EjbLink.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class SigninBean {

	private static Logger logger = Logger.getLogger(SigninBean.class);

	private HippoAccount account;

	private transient AccountSystem accountSystem;
	
	private EjbLink ejb;
	
	private void initAccountFromCookie() {				
		try {
			accountSystem = ejb.getEjb(AccountSystem.class);
			
			// this may well fail ... the whole point of this class is that it does really
			ejb.attemptLoginFromFacesContext();
			
			account = accountSystem.lookupAccountByPersonId(ejb.getLoggedInUser());
			
		} catch (BadTastingException e) {
			logger.info("invalid login: ", e);
		} catch (NotLoggedInException e) {
			account = null;
			logger.info("failed to login: ", e);
		}
		if (account != null)
			logger.info("logged in person \"" + account.getOwner() + "\" OK");			
	}

	public SigninBean() {
		ejb = new EjbLink();

		if (ejb == null)
			throw new RuntimeException("SigninBean could not create EjbLink");
		
		initAccountFromCookie();
	}
	
	private static String computeClientIdentifier() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		
		StringBuilder ret = new StringBuilder();
		ret.append(req.getRemoteAddr());
		String agent = req.getHeader("user-agent");
		if (agent != null) {
			ret.append('/');
			ret.append(agent);
		}
		
		return ret.toString();
	}
	
	public void initNewAccountFromEmail(String email) {
		UserTransaction tx = null;
		try {
			tx = (UserTransaction) new InitialContext().lookup("UserTransaction");			
			tx.begin();
			HippoAccount acct = accountSystem.createAccountFromEmail(email);
			initNewClient(accountSystem, acct);
			tx.commit();
			setAccount(acct);
		} catch (Exception e) {
			logger.error(e);
			try {
				if (tx != null)
					tx.rollback();
			} catch (SystemException e2) {
				// Ignore
			} catch (Exception e3) {
				throw new RuntimeException(e3);
			}
			throw new EJBException(e);
		}
	}
	
	public static Client initNewClient(HippoAccount account) {
		AccountSystem accounts;
		accounts = (new EjbLink()).getEjb(AccountSystem.class);
		return initNewClient(accounts, account);
	}

	public static Client initNewClient(AccountSystem accounts, HippoAccount account) {
		Client client = accounts.authorizeNewClient(account, computeClientIdentifier());
		
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
		LoginCookie loginCookie = new LoginCookie(account.getOwner().getId(), client.getAuthKey());
		response.addCookie(loginCookie.getCookie());
		return client;
	}

	public boolean isValid() {
		return account != null;
	}
	
	public HippoAccount getAccount() {
		return account;
	}

	public void setAccount(HippoAccount account) {
		this.account = account;
	}
}
