/**
 * 
 */
package com.dumbhippo.server.rewriters;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.dumbhippo.GlobalSetup;

class AmazonSaxHandler extends DefaultHandler {
	
	static private final Log logger = GlobalSetup.getLog(AmazonSaxHandler.class);
	
	// The enum names should match the xml element names (including case)
	enum Element { Item, ASIN,
		SmallImage, URL, Height, Width,
		OfferSummary,
		LowestNewPrice, LowestUsedPrice,
		LowestCollectiblePrice, LowestRefurbishedPrice,
		FormattedPrice,
		Error, Code, Message,
		IGNORED // an element we don't care about
	};
	
	private List<AmazonSaxHandler.Element> stack;
	private StringBuilder content;

	String ASIN;
	String smallImageUrl;
	int smallImageWidth;
	int smallImageHeight;
	private EnumMap<AmazonSaxHandler.Element,String> prices;
	
	AmazonSaxHandler() {
		stack = new ArrayList<AmazonSaxHandler.Element>();
		content = new StringBuilder();
	}
	
	private AmazonSaxHandler.Element current() {
		if (stack.size() > 0)
			return stack.get(stack.size() - 1);
		else
			return null;
	}

	private AmazonSaxHandler.Element parent() {
		if (stack.size() > 1)
			return stack.get(stack.size() - 2);
		else
			return null;
	}
	
	private AmazonSaxHandler.Element parseElementName(String name) {
		 AmazonSaxHandler.Element element;
		 try {
			 element = Element.valueOf(name);
		 } catch (IllegalArgumentException e) {
			 element = Element.IGNORED;
		 }
		 assert element != null;
		 return element;
	}
	
	private void push(String name) {
		 AmazonSaxHandler.Element element = parseElementName(name);
		 stack.add(element);			
	}
	
	private void pop(String name) throws SAXException {
		 AmazonSaxHandler.Element element = parseElementName(name);
		 AmazonSaxHandler.Element c = current();
		 if (c == null)
			 throw new SAXException("popped " + name + " when not in an element");
		 if (c != element)
			 throw new SAXException("unmatched close to element " + name + " we were expecting " + c.name());
		 stack.remove(stack.size() - 1);
		 content.setLength(0);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		 //logger.debug("start element " + qName);
		 push(qName);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		AmazonSaxHandler.Element c = current();
		
		if (c == Element.ASIN) {
			ASIN = content.toString();
		} else if (parent() == Element.SmallImage) {
			if (c == Element.URL) {
				smallImageUrl = content.toString();
			} else if (c == Element.Width) {
				String v = content.toString();
				smallImageWidth = Integer.parseInt(v);
			} else if (c == Element.Height) {
				String v = content.toString();
				smallImageHeight = Integer.parseInt(v);					
			}
		} else if (c == Element.FormattedPrice) {
			AmazonSaxHandler.Element p = parent();
			if (p != null) {
				String price = content.toString();
				if (prices == null) {
					prices = new EnumMap<AmazonSaxHandler.Element,String>(AmazonSaxHandler.Element.class);
				}
				logger.debug("saving price " + price + " for " + p);
				prices.put(p, price);
			}
		} else if (parent() == Element.Error) {
			// we don't throw or anything since the other fields 
			// will be absent and thus isValid() will return false,
			// assuming the error was fatal at least
			if (c == Element.Code) {
				logger.warn("Amazon error code " + content.toString());
			} else if (c == Element.Message) {
				logger.warn("Amazon error message " + content.toString());
			}
		}
		
		pop(qName);
		
		if (current() == null) {
			logger.debug("Parsed ASIN = " + ASIN + " prices = " + prices + " smallImageUrl = " + smallImageUrl + " " + smallImageWidth + "x" + smallImageHeight);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		AmazonSaxHandler.Element c = current();
		if (c != Element.IGNORED)
			content.append(ch, start, length);
	}

	boolean isValid() {
		return ASIN != null && prices != null && smallImageUrl != null && 
		smallImageWidth > 0 && smallImageHeight > 0;
	}
	
	String getASIN() {
		return ASIN;
	}

	String getNewPrice() {
		return prices.get(Element.LowestNewPrice);
	}

	String getUsedPrice() {
		return prices.get(Element.LowestUsedPrice);
	}

	String getCollectiblePrice() {
		return prices.get(Element.LowestCollectiblePrice);
	}

	String getRefurbishedPrice() {
		return prices.get(Element.LowestRefurbishedPrice);
	}
	
	String getSmallImageUrl() {
		return smallImageUrl;
	}

	int getSmallImageWidth() {
		return smallImageWidth;
	}

	int getSmallImageHeight() {
		return smallImageHeight;
	}
}
