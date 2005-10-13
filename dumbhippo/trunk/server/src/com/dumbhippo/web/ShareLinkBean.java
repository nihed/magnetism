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
import com.dumbhippo.server.IdentitySpider;

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
	
	private transient IdentitySpider identitySpider;
	
	public class RecipientsConverter implements Converter {

		private void throwError(String problem) {
			// FacesMessage has a "summary" and "detail" which 
			// is just useless basically
			FacesMessage message = new FacesMessage(problem, problem);
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ConverterException(message);
		}
		/*
		private Person attemptPersonFromString(String s) {
			
		}
		*/
		
		public Object getAsObject(FacesContext context, UIComponent component, String newValue) throws ConverterException {
			
			//		throwError("Sample error");
			
			// FIXME build list of person ID, not of unchanged strings
			List<String> object = new ArrayList<String>();
			String[] split = newValue.split(",");
			for (String s : split) {
				object.add(s.trim());
			}
			return object;
		}

		@SuppressWarnings("unchecked")
		public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
			
			if (value == null)
				return null;
			
			List<String> list = (List<String>) value;
			
			return StringUtils.join(list, ",");
		}
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
			
			logger.info("Sharing link!");
			// add the link to the database...
					
			
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