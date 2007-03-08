package com.dumbhippo;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RssBuilder {
	XmlBuilder xml = new XmlBuilder();
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);

	public boolean init = false;
	String title;
	URL link;
	String description;
	URL imageUrl;
	String imageTitle;
	URL imageLink;
	
	static final String GENERATOR = "Mugshot.org RSS Generator"; 
	
	public RssBuilder(String title, URL link, String description) {
		if (title == null)
			throw new NullPointerException("title is required");
		if (link == null)
			throw new NullPointerException("link is required");
		if (description == null)
			throw new NullPointerException("description is required");
		
		this.title = title;
		this.link = link;
		this.description = description;
	}
	
	public void setChannelImage(URL url, String title, URL link) {
		if (url == null)
			throw new NullPointerException("url is required");
		if (title == null)
			throw new NullPointerException("title is required");
		if (link == null)
			throw new NullPointerException("link is required");
		
		imageUrl = url;
		imageTitle= title;
		imageLink = link;
	}
	
	public void ensureInit() {
		if (!init) {
			init = true;
			
		    xml.appendStandaloneFragmentHeader();
		    xml.openElement("rss", "version", "2.0");

			xml.openElement("channel");
		    xml.appendTextNode("title", title);
		    xml.appendTextNode("link", link.toString());
		    xml.appendTextNode("description", description);
		    xml.appendTextNode("generator", GENERATOR);

		    if (imageUrl != null) {
		    	xml.openElement("image");
		    	xml.appendTextNode("url", imageUrl.toString());
		    	xml.appendTextNode("title", imageTitle);
		    	xml.appendTextNode("link", imageLink.toString());
		    	xml.closeElement();
		    }
		}
	}
	
	public void addItem(URL link, String title, String description, Date pubDate, String guid) {
		if (title == null && description == null)
			throw new RuntimeException("Either title or description must be present");
		
		ensureInit();
		
		xml.openElement("item");
		if (link != null)
			xml.appendTextNode("link", link.toString());
		if (title != null)
			xml.appendTextNode("title", title);
		if (description != null)
			xml.appendTextNode("description", description);
		if (pubDate != null) {
			synchronized(dateFormat) {
				xml.appendTextNode("pubDate", dateFormat.format(pubDate));
			}
		}
		if (guid != null)
			xml.appendTextNode("guid", guid);
		xml.closeElement();
	}
	
	@Override
	public String toString() {
		return xml.toString();
	}
	
	public byte[] getBytes() {
		return xml.getBytes();
	}
}
