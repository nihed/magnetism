package com.dumbhippo.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dumbhippo.StringUtils;
import com.dumbhippo.server.ShareLinkGlue;
import com.dumbhippo.server.UnknownPersonException;
import com.dumbhippo.web.EjbLink.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

/**
 * ShareLinkBean corresponds to the "share a link" JSF page.
 * 
 * @author dff hp
 */

public class ShareLinkBean {
	static Log logger = LogFactory.getLog(ShareLinkBean.class);

	private String url;

	private String description;

	// list of Person.getId()
	private List<String> recipients;
	
	private transient ShareLinkGlue cachedGlue;
	
	public class RecipientsConverter implements Converter {

		private ConverterException postError(String problem) {
			// FacesMessage has a "summary" and "detail" which 
			// is just useless basically
			FacesMessage message = new FacesMessage(problem, problem);
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			return new ConverterException(message);
		}
		
		public Object getAsObject(FacesContext context, UIComponent component, String newValue) throws ConverterException {
			
			ShareLinkGlue glue = getGlue();
			
			// FIXME build list of person ID, not of unchanged strings
			List<String> freeforms = new ArrayList<String>();
			String[] split = newValue.split(",");
			for (String s : split) {
				freeforms.add(s.trim());
			}
			
			try {
				return glue.freeformRecipientsToIds(freeforms);
			} catch (UnknownPersonException e) {
				e.printStackTrace();
				throw postError(e.getMessage());
			}
		}

		@SuppressWarnings("unchecked")
		public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
			
			if (value == null)
				return null;
			
			List<String> list = (List<String>) value;
			
			return StringUtils.join(list, ",");
		}
	}

	private ShareLinkGlue getGlue() {
		if (cachedGlue == null) {
			EjbLink ejb = new EjbLink();
			try {
				ejb.attemptLoginFromFacesContext();
				cachedGlue = ejb.nameLookup(ShareLinkGlue.class);
			} catch (BadTastingException e) {
				e.printStackTrace();
				logger.error("Failed to login (bad cookie)", e);
			} catch (NotLoggedInException e) {
				e.printStackTrace();
				logger.error("Failed to login (not logged in)", e);
			}
		}
		if (cachedGlue == null) {
			throw new IllegalStateException("Need to be logged in to share a link");
		}
		return cachedGlue;
	}
	
	public ShareLinkBean() {
		
	}
	
	public Converter getRecipientsConverter() {
		return new RecipientsConverter();
	}
	
	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<String> recipients) {
		logger.info("Set recipients = " + recipients);
		this.recipients = recipients;
	}
		
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		logger.info("Set url = " + url);
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		logger.info("Set description = " + description);
		this.description = description;
	}
	
	// action handler for form submit
	public String doShareLink() {
		try {
			
			if (url == null || description == null || recipients == null) {
				throw new IllegalStateException("Not all fields provided for link share");
			}
			
			logger.info("Sharing link!");
			
			ShareLinkGlue glue = getGlue();
			
			glue.shareLink(url, recipients, description);
			
			return "main";
		} catch (Exception e) {
			logger.debug(e);
			// didn't work for some reason, just reload the page
			// (should have our JSF message queue displayed in there in theory,
			// with the errors)
			return null;
		}
	}
}