package com.dumbhippo.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.server.ShareLinkGlue;
import com.dumbhippo.server.UnknownPersonException;

/**
 * ShareLinkBean corresponds to the "share a link" JSF page.
 * 
 * @author dff hp
 */

public class ShareLinkBean {
	static private final Log logger = GlobalSetup.getLog(ShareLinkBean.class);

	private String url;

	private String description;

	// list of Person.getId()
	private List<String> recipients;
	
	@Inject
	private transient EjbLink ejb;
	
	@Inject
	private transient ShareLinkGlue shareLinkGlue;
	
	public class RecipientsConverter implements Converter {

		private ConverterException postError(String problem) {
			// FacesMessage has a "summary" and "detail" which 
			// is just useless basically
			FacesMessage message = new FacesMessage(problem, problem);
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			return new ConverterException(message);
		}
		
		public Object getAsObject(FacesContext context, UIComponent component, String newValue) throws ConverterException {
			
			List<String> freeforms = new ArrayList<String>();
			String[] split = newValue.split(",");
			for (String s : split) {
				freeforms.add(s.trim());
			}
			
			// if we aren't logged in yet, I don't think we have a FacesContext
			// here so we're hosed...
			if (!ejb.checkLoginFromFacesContext(this))
				return null;
			
			try {
				return shareLinkGlue.freeformRecipientsToIds(freeforms);
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
	
	public ShareLinkBean() {
		EjbLink.injectFromFacesContext(this, Scope.NONE);
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
			if (!ejb.checkLoginFromFacesContext(this)) {
				return "login";
			}
			
			if (url == null || description == null || recipients == null) {
				throw new IllegalStateException("Not all fields provided for link share");
			}
			
			logger.info("Sharing link!");
			
			shareLinkGlue.shareLink(url, recipients, description);
			
			return "sharelinkcomplete";
		} catch (Exception e) {
			logger.debug(e);
			// didn't work for some reason, just reload the page
			// (should have our JSF message queue displayed in there in theory,
			// with the errors)
			return null;
		}
	}
}
