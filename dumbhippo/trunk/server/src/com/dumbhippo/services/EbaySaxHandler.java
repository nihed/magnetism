package com.dumbhippo.services;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

public class EbaySaxHandler extends EnumSaxHandler<EbaySaxHandler.Element> implements EbayItemData {

	static private final Log logger = GlobalSetup.getLog(EbaySaxHandler.class);
	
	// The enum names should match the xml element names (including case).
	// indentation roughly matches xml nesting ;-)
	enum Element {
		Ack,
		Timestamp,
		RuName,
		Errors,
			ShortMessage,
			LongMessage,
			SeverityCode,
			ErrorClassification,
		Item,
			ListingDetails,
				ConvertedBuyItNowPrice,
				ConvertedStartPrice,
				ConvertedReservePrice,
				HasReservePrice,
				StartTime,
				EndTime,
				ViewItemURL,
			SiteHostedPicture,
				PictureUrl,
			VendorHostedPicture,
				SelfHostedUrl,
				GalleryUrl,
			Title,
			TimeLeft,
			StartPrice,
		IGNORED // an element we don't care about
	};	

	private EnumMap<Element,String> values;
	private Date cachedTimestamp;
	
	EbaySaxHandler() {
		super(Element.class, Element.IGNORED);
		values = new EnumMap<Element,String>(Element.class);
	}

	@Override
	protected void closeElement(Element c) throws SAXException {
		if (c == Element.Title) {
			values.put(c, getCurrentContent());
		} else if (c == Element.TimeLeft) {
			values.put(c, getCurrentContent());
		} else if (c == Element.StartPrice) {
			values.put(c, getCurrentContent());
		} else if (parent() == Element.ListingDetails) {
			// not really using any of the sub-nodes of ListingDetails
		} else if (parent() == Element.SiteHostedPicture) {
			if (c != Element.IGNORED)
				values.put(c, getCurrentContent());
		} else if (parent() == Element.VendorHostedPicture) {
			if (c != Element.IGNORED)
				values.put(c, getCurrentContent());	
		} else if (parent() == Element.Errors) {
			if (c != Element.IGNORED)
				logger.debug("eBay error:" + c + ": '" + getCurrentContent() + "'");
		} else if (c == Element.RuName) {
			logger.debug("RuName = " + getCurrentContent());
		} else if (c == Element.Timestamp) {
			values.put(c, getCurrentContent());
		}
	}
	
	private boolean isValid() {
		// strict validation, yeehaw
		return !values.isEmpty();
	}
	
	@Override 
	public void endDocument() throws SAXException {
		logger.debug("Ebay fields loaded " + values);
		if (!isValid())
			throw new SAXException("Missing needed ebay fields");
	}

	public String getPictureUrl() {
		String url = values.get(Element.PictureUrl);
		if (url == null)
			url = values.get(Element.GalleryUrl);
		if (url == null)
			url = values.get(Element.SelfHostedUrl);
		return url;
	}

	public String getTimeLeft() {
		return values.get(Element.TimeLeft);
	}

	public String getBuyItNowPrice() {
		return values.get(Element.ConvertedBuyItNowPrice);
	}

	public String getStartPrice() {
		return values.get(Element.ConvertedStartPrice);
	}
	
	public Date getTimestamp() {
		if (cachedTimestamp == null) {
			String s = values.get(Element.Timestamp);
			if (s != null) {
				try {
					DateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					TimeZone tz = TimeZone.getTimeZone("UTC");
					parser.setTimeZone(tz);
					cachedTimestamp = parser.parse(s);
				} catch (ParseException e) {
					logger.warn("Could not parse ebay date '" + s + "': " + e.getMessage());
				}
			}
		}
		return cachedTimestamp;
	}
}
