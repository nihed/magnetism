/**
 * 
 */
package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

class AmazonItemLookupSaxHandler extends EnumSaxHandler<AmazonItemLookupSaxHandler.Element> {
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonItemLookupSaxHandler.class);
	
	// The enum names should match the xml element names (including case)
	enum Element {
		Item, ASIN,
		Title, 
		EditorialReview,
		    Source,
		    Content,
		SmallImage, URL, Height, Width,
		OfferSummary,
		LowestNewPrice, LowestUsedPrice,
		LowestCollectiblePrice, LowestRefurbishedPrice,
		FormattedPrice,
		Error, Code, Message,
		IGNORED // an element we don't care about
	};	

	private List<AmazonItem> items;
	private String errorCode;
	private String errorMessage;
	
	AmazonItemLookupSaxHandler() {
		super(Element.class, Element.IGNORED);
		items = new ArrayList<AmazonItem>();
	}

	private int parseCount(Element c, String content) {
	    try {	
			return Integer.parseInt(content);			
		} catch (NumberFormatException e) {
			logger.warn("Amazon web services content {} for element {} was not a valid number",
					    content, c);
			return -1;
		}
	}
	
	private AmazonItem currentItem() {
		if (items.size() > 0)
			return items.get(items.size() - 1);
		else
			return null;
	}
	
	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.Item) {
			AmazonItem item = new AmazonItem();
			items.add(item);
		}
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {
		String currentContent = getCurrentContent().trim();  
		if (c == Element.ASIN) {
			if (currentItem() != null)
				currentItem().setItemId(currentContent);
		} else if (c == Element.Title) {
			currentItem().setTitle(currentContent);
		} else if (c == Element.Content && parent() == Element.EditorialReview)	{
			currentItem().setEditorialReview(currentContent);
		} else if (parent() == Element.SmallImage) {
			if (c == Element.URL) {
				currentItem().setSmallImageUrl(currentContent);
			} else if (c == Element.Width) {
				currentItem().setSmallImageWidth(parseCount(c, currentContent));
			} else if (c == Element.Height) {
				currentItem().setSmallImageHeight(parseCount(c, currentContent));		
			}
		} else if (c == Element.FormattedPrice) {
			Element p = parent();
			if (p != null) {
				switch (p) {
				    case LowestNewPrice :
				    	currentItem().setNewPrice(currentContent);
				  	    break;
				    case LowestUsedPrice :
				    	currentItem().setUsedPrice(currentContent);
					    break;
				    case LowestCollectiblePrice :
				    	currentItem().setCollectiblePrice(currentContent);
				    	break;
				    case LowestRefurbishedPrice :
				    	currentItem().setCollectiblePrice(currentContent);
				    	break;
				}
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
	
	public List<AmazonItem> getItems() {
		return items;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
}
