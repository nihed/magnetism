/**
 * 
 */
package com.dumbhippo.server.rewriters;

import java.util.EnumMap;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

class AmazonSaxHandler extends EnumSaxHandler<AmazonSaxHandler.Element> implements AmazonItemData {
	
	static private final Log logger = GlobalSetup.getLog(AmazonSaxHandler.class);
	
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

	String ASIN;
	String smallImageUrl;
	int smallImageWidth;
	int smallImageHeight;
	private EnumMap<Element,String> prices;
	
	AmazonSaxHandler() {
		super(Element.class, Element.IGNORED);
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
				if (prices == null) {
					prices = new EnumMap<Element,String>(Element.class);
				}
				logger.debug("saving price " + price + " for " + p);
				prices.put(p, price);
			}
		} else if (parent() == Element.Error) {
			// we don't throw or anything since the other fields 
			// will be absent and thus isValid() will return false,
			// assuming the error was fatal at least
			if (c == Element.Code) {
				logger.warn("Amazon error code " + getCurrentContent());
			} else if (c == Element.Message) {
				logger.warn("Amazon error message " + getCurrentContent());
			}
		}
	}
	
	@Override 
	public void endDocument() throws SAXException {
		logger.debug("Parsed ASIN = " + ASIN + " prices = " + prices + " smallImageUrl = " + smallImageUrl + " " + smallImageWidth + "x" + smallImageHeight);
		if (!isValid())
			throw new SAXException("Missing needed amazon fields");
	}
	
	private boolean isValid() {
		return ASIN != null && prices != null && smallImageUrl != null && 
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
