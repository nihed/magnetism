package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.web.WebEJBUtil;
;

public abstract class AbstractFlashTag extends SimpleTagSupport {
	
	private static final String DEFAULT_BACKGROUND = "#0099cc";

	private String userId;
	private boolean escapeXml;
	private boolean hasLabel;
	private boolean embedOnly;

	protected AbstractFlashTag() {
		hasLabel = true;
	}
	
	public void setUserId(String userId) {
		try {
			// paranoia since we output this again and it could be provided
			// by a user
			Guid.validate(userId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		this.userId = userId;
	}

	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}

	public void setHasLabel(boolean hasLabel) {
		this.hasLabel = hasLabel;
	}

	public void setEmbedOnly(boolean embedOnly) {
		this.embedOnly = embedOnly;
	}	
	
	static private String getAbsoluteUrl(FlashBadge badge, String queryString) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String baseurl = config.getProperty(HippoProperty.BASEURL);
		StringBuilder sb = new StringBuilder();
		sb.append(baseurl);
		sb.append(badge.getRelativeUrl());
		sb.append("?");
		sb.append(queryString);
		return sb.toString();
	}
	
	static public String getEmbedHtml(FlashBadge badge, String queryString, String bgColor, String label) {
		if (bgColor == null)
			bgColor = DEFAULT_BACKGROUND;
		
		// try to keep this nicely formatted, since we give it to people to cut-and-paste
		String format = ""
		+ "<div align=\"center\">\n"
		+ "%s"
		+ "<embed quality=\"high\" wmode=\"transparent\" bgcolor=\"%s\"\n"
		+ "       width=\"%d\" height=\"%d\" align=\"middle\"\n"
		+ "       allowScriptAccess=\"sameDomain\" type=\"application/x-shockwave-flash\"\n"
		+ "       pluginspage=\"http://www.macromedia.com/go/getflashplayer\"\n"
		+ "       src=\"%s\" />\n"
		+ "</div>\n";
		return String.format(format, label != null ? label : "", 
				bgColor, badge.getWidth(), badge.getHeight(),
				getAbsoluteUrl(badge, queryString));
	}
	
	static public String getObjectHtml(FlashBadge badge, String queryString, String bgColor, String label) {
		
		if (bgColor == null)
			bgColor = DEFAULT_BACKGROUND;
		
		// try to keep this nicely formatted, since we give it to people to cut-and-paste
		String format = "%s" 
		+ "<object classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\"\n"
		+ "        codebase=\"http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=%d,0,0,0\"\n"
		+ "        width=\"%d\" height=\"%d\" align=\"middle\">\n"
		+ "  <param name=\"allowScriptAccess\" value=\"sameDomain\" />\n"
		+ "  <param name=\"quality\" value=\"high\" />\n"
		+ "  <param name=\"wmode\" value=\"transparent\" />\n"
		+ "  <param name=\"bgcolor\" value=\"%s\" />\n"
		+ "  <param name=\"movie\" value=\"%s\" />\n"
		+ "%s" // <embed> tag
		+ "</object>\n";
		
		// note that we always pass null for the label to getEmbedHtml or we'd get a double label in Firefox
		return String.format(format, label != null ? label : "", badge.getMinFlashVersion(),
				badge.getWidth(), badge.getHeight(),
				bgColor,
				getAbsoluteUrl(badge, queryString), getEmbedHtml(badge, queryString, bgColor, null));
	}

	protected final void doTag(FlashBadge badge, String divClass, String bgColor, String label, String... queryParamPairs) throws IOException {
		if (userId == null)
			throw new RuntimeException("no user provided to output tag");

		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String baseurl = config.getProperty(HippoProperty.BASEURL);		
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("who=");
		sb.append(userId);
		
		sb.append("&");
		sb.append("baseUrl=");
		sb.append(StringUtils.urlEncode(baseurl));
		
		if ((queryParamPairs.length % 2) != 0)
			throw new IllegalArgumentException("query params need key-value pairs");
		
		for (int i = 0; i < queryParamPairs.length; i += 2) {
			String key = queryParamPairs[i];
			String value = queryParamPairs[i+1];
			
			if (value != null) {
				sb.append("&");
				
				sb.append(key);
				sb.append("=");
				sb.append(StringUtils.urlDecode(value));
			}
		}
	
		String queryString = sb.toString();
		
		JspWriter writer = getJspContext().getOut();
		String output;
		if (embedOnly)
			output = getEmbedHtml(badge, queryString, bgColor, hasLabel ? label : null);
		else
			output = getObjectHtml(badge, queryString, bgColor, hasLabel ? label : null);
		
		// put a <div> around it if we're rendering the HTML ourselves, if we're printing out the html 
		// for others then don't
		if (escapeXml)
			output = XmlBuilder.escape(output);
		else 
			output = "<div class=\"" + divClass + "\">" + output + "</div>";
		writer.print(output);
	}
}
