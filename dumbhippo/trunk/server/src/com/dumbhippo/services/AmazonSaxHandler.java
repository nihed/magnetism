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
		CustomerReviews,
		    TotalReviews,
		    TotalReviewPages,
		    Review,
		        ASIN,
		        Rating,
		        HelpfulVotes,
		        TotalVotes,
		        Date,
		        Summary,
		        Content,	
		        // TODO: add error message handling
		IGNORED // an element we don't care about
	};	
	
	private String amazonUserId;
	private boolean forTotalCount;
	private int totalReviews;
	private AmazonReviews reviews;
	private AmazonReview currentReview;
	
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
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd");
		try {
			return dateFormat.parse(content);
		} catch (ParseException e) {
			logger.warn("Failed to parse Amazon web services content {} for element {} as a 'yyyy MM dd' date",
				        content, c);
			return new Date(-1);
		}
	}
	
	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.CustomerReviews) {
			if (reviews != null)
				throw new SAXException("two <CustomerReviews> elements in response");
            reviews = new AmazonReviews();
		} else if (c == Element.Review && !forTotalCount) {
		    currentReview = new AmazonReview();
			currentReview.setAmazonUserId(amazonUserId);
		} 
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {		
		String currentContent = getCurrentContent().trim();  
		if (c == Element.TotalReviews) {
	    	totalReviews = parseCount(c, currentContent);
	    } else if (c == Element.CustomerReviews) {
	    	// We set totalReviews when we are closing CustomerReviews, and not when
	    	// we encounter TotalReviews, because AmazonReviews would otherwise 
	    	// reset it when we add the reviews one by one, and we don't want that
	    	// because totalReviews might be greater than the number of review
	    	// objects returned by Amazon web services.
	    	reviews.setTotal(totalReviews);
	    } else if (forTotalCount) {
	    	// we are not interested in anything else
	    	return;
	    } else if (c == Element.CustomerId) {
			amazonUserId = currentContent;
		} else if (c == Element.TotalReviews) {
	    	totalReviews = parseCount(c, currentContent);
	    } else if (c == Element.TotalReviewPages) {
	    	reviews.setTotalReviewPages(parseCount(c, currentContent));
	    } else if (c == Element.ASIN) {
	    	currentReview.setItemId(currentContent);
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
	    	reviews.addReview(currentReview);
	    	currentReview = null;
	    }
	}
	
	public AmazonReviews getReviews() {
		return reviews;
	}
}
