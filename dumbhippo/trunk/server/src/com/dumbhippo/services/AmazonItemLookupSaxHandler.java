/**
 * 
 */
package com.dumbhippo.services;

import java.util.EnumMap;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

class AmazonItemLookupSaxHandler extends EnumSaxHandler<AmazonItemLookupSaxHandler.Element> implements AmazonItemData {
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonItemLookupSaxHandler.class);
	
	// The enum names should match the xml element names (including case)
	enum Element {
		Item, ASIN,
		SmallImage, URL, Height, Width,
		OfferSummary,
		LowestNewPrice, LowestUsedPrice,
		LowestCollectiblePrice, LowestRefurbishedPrice,
		FormattedPrice,
		Error, Code, Message,
		IGNORED // an element we don't care about
	};	

	private String ASIN;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	private EnumMap<Element,String> prices;
	private String errorCode;
	private String errorMessage;
	
	AmazonItemLookupSaxHandler() {
		super(Element.class, Element.IGNORED);
		prices = new EnumMap<Element,String>(Element.class);
	}

	@Override
	protected void closeElement(Element c) throws SAXException {
		
		if (c == Element.ASIN) {
			ASIN = getCurrentContent();
		} else if (parent() == Element.SmallImage) {
			if (c == Element.URL) {
				smallImageUrl = getCurrentContent();
			} else if (c == Element.Width) {
				String v = getCurrentContent();
				smallImageWidth = Integer.parseInt(v);
			} else if (c == Element.Height) {
				String v = getCurrentContent();
				smallImageHeight = Integer.parseInt(v);					
			}
		} else if (c == Element.FormattedPrice) {
			Element p = parent();
			if (p != null) {
				String price = getCurrentContent();
				//logger.debug("saving price " + price + " for " + p);
				prices.put(p, price);
			}
		} else if (parent() == Element.Error) {
			// we don't throw or anything since the other fields 
			// will be absent and thus isValid() will return false,
			// assuming the error was fatal at least
			if (c == Element.Code) {
				errorCode = getCurrentContent();
				logger.debug("Amazon error code {}", errorCode);
			} else if (c == Element.Message) {
				errorMessage = getCurrentContent();
				logger.debug("Amazon error message {}", errorMessage);
			}
		}
	}
	
	@Override 
	public void endDocument() throws SAXException {
		//logger.debug("Parsed ASIN = " + ASIN + " prices = " + prices + " smallImageUrl = " + smallImageUrl + " " + smallImageWidth + "x" + smallImageHeight);
		if (!isValid()) {
			if (errorCode != null) {
				boolean unexpected = !errorCode.equals("AWS.ECommerceService.NoExactMatches");
				throw new ServiceException(unexpected, errorCode + (errorMessage != null ? errorMessage : "no message"));
			} else {
	 			throw new SAXException("Missing needed amazon fields");
			}
		}
	}
	
	private boolean isValid() {
		// prices can all be absent if the product is sold out, so we don't 
		// validate that we have prices.
		return ASIN != null && smallImageUrl != null && 
		smallImageWidth > 0 && smallImageHeight > 0;
	}
	
	public String getASIN() {
		return ASIN;
	}

	public String getNewPrice() {
		return prices.get(Element.LowestNewPrice);
	}

	public String getUsedPrice() {
		return prices.get(Element.LowestUsedPrice);
	}

	public String getCollectiblePrice() {
		return prices.get(Element.LowestCollectiblePrice);
	}

	public String getRefurbishedPrice() {
		return prices.get(Element.LowestRefurbishedPrice);
	}
	
	public String getSmallImageUrl() {
		return smallImageUrl;
	}

	public int getSmallImageWidth() {
		return smallImageWidth;
	}

	public int getSmallImageHeight() {
		return smallImageHeight;
	}
}
