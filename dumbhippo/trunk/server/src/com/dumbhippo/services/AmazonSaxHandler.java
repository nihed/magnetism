package com.dumbhippo.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

public class AmazonSaxHandler extends EnumSaxHandler<AmazonSaxHandler.Element> {
	static private final Logger logger = GlobalSetup.getLogger(AmazonSaxHandler.class);

	enum Element {
		CustomerId,
		WishListId,
		List,
		    ListId,
		    ListName,
		    TotalItems,
		    TotalPages,
		    DateCreated,
		    ListItem,
		        ListItemId,
		        DateAdded,
		        Comment,
		        QuantityDesired,
		        QuantityReceived,		        
		CustomerReviews,
		    TotalReviews,
		    TotalReviewPages,
		    Review,
		        Rating,
		        HelpfulVotes,
		        TotalVotes,
		        Date,
		        Summary,
		        Content,	
		ASIN, // can be found both under Review and under ListItem/Item       
		// TODO: add error message handling
		IGNORED // an element we don't care about
	};	
	
	private String amazonUserId;
	private boolean forTotalCount;
	private AmazonReviews reviews;
	private AmazonReview currentReview;
	private AmazonLists lists;
	private AmazonList list;
	private AmazonListItem currentListItem;
	
	AmazonSaxHandler() {
		this(false);
	}
	
	AmazonSaxHandler(boolean forTotalCount) {
		super(Element.class, Element.IGNORED);
		this.forTotalCount = forTotalCount;
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
	
	private Date parseAmazonDate(Element c, String content) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return dateFormat.parse(content);
		} catch (ParseException e) {
			logger.warn("Failed to parse Amazon web services content {} for element {} as a 'yyyy-MM-dd' date",
				        content, c);
			return new Date(-1);
		}
	}
	
	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.List) {
			if (list != null)
				throw new SAXException("two <List> elements in response");
			list = new AmazonList();
		} if (c == Element.CustomerReviews) {
			if (reviews != null)
				throw new SAXException("two <CustomerReviews> elements in response");
            reviews = new AmazonReviews();
		} else if (c == Element.Review && !forTotalCount) {
		    currentReview = new AmazonReview();
			currentReview.setAmazonUserId(amazonUserId);
		} else if (c == Element.ListItem) {
			currentListItem = new AmazonListItem();
		}
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {		
		String currentContent = getCurrentContent().trim();  
		if (c == Element.TotalReviews) {
			reviews.setTotal(parseCount(c, currentContent));
        } else if (forTotalCount) {
	    	// we are not interested in anything else
	    	return;
	    } else if (c == Element.CustomerId) {
			amazonUserId = currentContent;
		} else if (c == Element.TotalReviewPages) {
	    	reviews.setTotalReviewPages(parseCount(c, currentContent));
	    } else if (c == Element.ASIN) {
	    	if (currentReview != null) {
	    	    currentReview.setItemId(currentContent);
	    	} else if (currentListItem != null) {
	    		currentListItem.setItemId(currentContent);
	    	}
	    } else if (c == Element.Rating) {
	    	currentReview.setRating(parseCount(c, currentContent));
	    } else if (c == Element.HelpfulVotes) {
	    	currentReview.setHelpfulVotes(parseCount(c, currentContent));
	    } else if (c == Element.TotalVotes) {
	    	currentReview.setTotalVotes(parseCount(c, currentContent));
	    } else if (c == Element.Date) {
	    	currentReview.setReviewDate(parseAmazonDate(c, currentContent));	    	
	    } else if (c == Element.Summary) {
	    	currentReview.setTitle(currentContent);
	    } else if (c == Element.Content) {
	    	currentReview.setContent(currentContent);
	    } else if (c == Element.Review) {
	    	// Total reviews number might be greater than the number of review
	    	// objects returned by Amazon web services, so we don't want adding
	    	// a review to refresh the total count.
	    	reviews.addReview(currentReview, false);
	    	currentReview = null;
	    } else if (c == Element.WishListId) {
			if (lists == null) 
				lists = new AmazonLists();		
			AmazonList listWithId = new AmazonList();
			listWithId.setListId(currentContent);
			lists.addList(listWithId, true);			
		} else if (c == Element.ListId) {
			// ListId first occures when the request parameters are repeated
			// in the response, we can ignore it then
			if (list != null) {
			    list.setListId(currentContent);
			}
		} else if (c == Element.ListName) {
			list.setListName(currentContent);
		} else if (c == Element.TotalItems) {
			list.setTotalItems(parseCount(c, currentContent));			
		} else if (c == Element.TotalPages) {
			list.setTotalPages(parseCount(c, currentContent));
		} else if (c == Element.DateCreated) {
			list.setDateCreated(parseAmazonDate(c, currentContent));
		} else if (c == Element.ListItemId) {
			currentListItem.setListItemId(currentContent);			
		} else if (c == Element.DateAdded) {
			currentListItem.setDateAdded(parseAmazonDate(c, currentContent));
		} else if (c == Element.Comment) {
			currentListItem.setComment(currentContent);
		} else if (c == Element.QuantityDesired) {
			currentListItem.setQuantityDesired(parseCount(c, currentContent));
		} else if (c == Element.QuantityReceived) {
			currentListItem.setQuantityReceived(parseCount(c, currentContent));
		} else if (c == Element.ListItem) {
		    list.addListItem(currentListItem);
		    currentListItem = null;
		}
	}
	
	public AmazonReviews getReviews() {
		return reviews;
	}
	
	public AmazonLists getLists() {
		return lists;
	}
	
	public AmazonList getList() {
		return list;
	}
}
