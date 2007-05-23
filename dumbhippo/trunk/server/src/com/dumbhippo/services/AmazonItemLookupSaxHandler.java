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
		Image, MediumImage, URL, Height, Width,
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
	
	private String parseEditorialReview(String content) {
	    // remove tags and truncate to the maximum length we can store in the database
		StringBuilder sb = new StringBuilder();
		int currentIndex = 0;
		while (((content.indexOf("<", currentIndex) >= 0) && sb.length() < AmazonItemView.MAX_EDITORIAL_REVIEW_LENGTH)) {
			sb.append(content.substring(currentIndex, content.indexOf("<", currentIndex)));
			// returned content should not have unclosed tags, so if we see a "<" without a matching ">", we can
			// just leave it in place
			if (content.indexOf(">", currentIndex + 1) > 0) {	
			    currentIndex = content.indexOf(">", currentIndex + 1) + 1;
			} else {
				currentIndex = content.indexOf("<", currentIndex);
			}
		}
		
		// if there are no more tags in the editorial review, but we have not reached the max length,
		// just append the rest of the review to the string buffer
		if (((currentIndex < content.length()) && (content.indexOf("<", currentIndex) < 0) 
			&& sb.length() < AmazonItemView.MAX_EDITORIAL_REVIEW_LENGTH)) {
		    sb.append(content.substring(currentIndex, content.length()));	
		}
		
		sb.setLength(Math.min(sb.length(), AmazonItemView.MAX_EDITORIAL_REVIEW_LENGTH));
		return sb.toString();
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
			currentItem().setEditorialReview(parseEditorialReview(currentContent));
        // a product can have multiple images of different views, we are nly interested
	    // in getting the first one
		} else if (parent() == Element.MediumImage) {
			if (currentItem().getImageUrl() == null || currentItem().getImageUrl().trim().length() == 0) {
				logger.debug("current item getImageUrl is null or empty");
			} else {
				logger.debug("getImageUrl {}", currentItem().getImageUrl());
			}
			if (c == Element.URL) {
				currentItem().setImageUrl(currentContent);
			} else if (c == Element.Width) {
				currentItem().setImageWidth(parseCount(c, currentContent));
			} else if (c == Element.Height) {
				currentItem().setImageHeight(parseCount(c, currentContent));		
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
				    default:
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
