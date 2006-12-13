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

public class NowPlayingTag extends SimpleTagSupport {

	private static final String DEFAULT_BACKGROUND = "#0099cc";
	
	private String userId;
	private String themeId;	
	private boolean escapeXml;
	private String forceMode;
	private boolean hasLabel;
	private boolean embedOnly;
	
	public NowPlayingTag() {
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
	
	public void setThemeId(String themeId) {
		try {
			// paranoia since we output this again and it could be provided
			// by a user
			Guid.validate(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		this.themeId = themeId;	
	}

	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}
	
	public void setForceMode(String forceMode) {
		this.forceMode = forceMode;
	}
	
	public void setHasLabel(boolean hasLabel) {
		this.hasLabel = hasLabel;
	}
	
	public void setEmbedOnly(boolean embedOnly) {
		this.embedOnly = embedOnly;
	}
	
	static private String getSrc(String userId, String themeId, String forceMode) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String baseurl = config.getProperty(HippoProperty.BASEURL);
		StringBuilder sb = new StringBuilder();
		sb.append(baseurl);
		sb.append("/flash/nowPlaying.swf?who=");
		sb.append(userId);
		sb.append("&baseUrl=" + StringUtils.urlEncode(baseurl));
		if (themeId != null) {
			sb.append("&theme=");
			sb.append(themeId);
		}
		if (forceMode != null) {
			sb.append("&forceMode=");
			sb.append(StringUtils.urlEncode(forceMode));
		}
		return sb.toString();
	}
	
	static final private String LABEL = "<b>What I'm listening to right now</b>\n";
	
	static public String getNowPlayingEmbedHtml(String userId, String themeId, String forceMode, String bgColor, boolean hasLabel) {
		if (bgColor == null)
			bgColor = DEFAULT_BACKGROUND;
		
		// try to keep this nicely formatted, since we give it to people to cut-and-paste
		String format = ""
		+ "<div align=\"center\">\n"
		+ "%s"
		+ "<embed quality=\"high\" wmode=\"transparent\" bgcolor=\"%s\"\n"
		+ "       width=\"440\" height=\"120\" name=\"nowPlaying\" align=\"middle\"\n"
		+ "       allowScriptAccess=\"sameDomain\" type=\"application/x-shockwave-flash\"\n"
		+ "       pluginspage=\"http://www.macromedia.com/go/getflashplayer\"\n"
		+ "       src=\"%s\" />\n"
		+ "</div>\n";
		return String.format(format, hasLabel ? LABEL : "", 
				bgColor, getSrc(userId, themeId, forceMode));
	}
	
	static public String getNowPlayingObjectHtml(String userId, String themeId, String forceMode, String bgColor, boolean hasLabel) {
		
		if (bgColor == null)
			bgColor = DEFAULT_BACKGROUND;
		
		// try to keep this nicely formatted, since we give it to people to cut-and-paste
		String format = "%s" 
		+ "<object classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\"\n"
		+ "        codebase=\"http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=7,0,0,0\"\n"
		+ "        width=\"440\" height=\"120\" align=\"middle\">\n"
		+ "  <param name=\"allowScriptAccess\" value=\"sameDomain\" />\n"
		+ "  <param name=\"quality\" value=\"high\" />\n"
		+ "  <param name=\"wmode\" value=\"transparent\" />\n"
		+ "  <param name=\"bgcolor\" value=\"%s\" />\n"
		+ "  <param name=\"movie\" value=\"%s\" />\n"
		+ "%s" // <embed> tag
		+ "</object>\n";
		
		return String.format(format, hasLabel ? LABEL : "", bgColor, getSrc(userId, themeId, forceMode), getNowPlayingEmbedHtml(userId, themeId, forceMode, bgColor, false));
	}
	
	@Override
	public void doTag() throws IOException {
		if (userId == null)
			throw new RuntimeException("no user provided to NowPlayingTag");
		
		JspWriter writer = getJspContext().getOut();
		String output;
		if (embedOnly)
			output = getNowPlayingEmbedHtml(userId, themeId, forceMode, DEFAULT_BACKGROUND, hasLabel);
		else
			output = getNowPlayingObjectHtml(userId, themeId, forceMode, DEFAULT_BACKGROUND, hasLabel);
		
		// put a <div> around it if we're rendering the HTML ourselves, if we're printing out the html 
		// for others then don't
		if (escapeXml)
			output = XmlBuilder.escape(output);
		else 
			output = "<div class=\"dh-nowplaying\">" + output + "</div>";
		writer.print(output);
	}
}
