package com.dumbhippo.web;

import com.dumbhippo.StringUtils;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class NowPlayingPage {

	@Signin
	private SigninBean signin;
	
	private Configuration config;
	
	public NowPlayingPage() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	private String getBase() {
		return config.getProperty(HippoProperty.BASEURL);
	}
	
	private String getSrc() {
		return getBase() + "/flash/nowPlaying.swf?who=" + signin.getUserId() + "&baseUrl=" + StringUtils.urlEncode(getBase()); 
	}
	
	private String getBgColor() {
		return "#0099cc";
	}
	
	public String getNowPlayingEmbedHtml() {
		// try to keep this nicely formatted, since we give it to people to cut-and-paste
		String format = ""
		+ "<div align=\"center\">\n"
		+ "<strong>What I'm listening to right now</strong>\n"
		+ "<embed quality=\"high\" wmode=\"transparent\" bgcolor=\"%s\"\n"
		+ "       width=\"440\" height=\"120\" name=\"nowPlaying\" align=\"middle\"\n"
		+ "       allowScriptAccess=\"sameDomain\" type=\"application/x-shockwave-flash\"\n"
		+ "       pluginspage=\"http://www.macromedia.com/go/getflashplayer\"\n"
		+ "       src=\"%s\" />\n"
		+ "</div>\n";
		return String.format(format, getBgColor(), getSrc());
	}
	
	public String getNowPlayingObjectHtml() {
		// try to keep this nicely formatted, since we give it to people to cut-and-paste
		String format = "" 
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
		
		return String.format(format, getBgColor(), getSrc(), getNowPlayingEmbedHtml());
	}
}
