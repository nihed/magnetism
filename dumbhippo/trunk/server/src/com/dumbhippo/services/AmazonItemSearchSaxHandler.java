package com.dumbhippo.services;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;


class AmazonItemSearchSaxHandler extends EnumSaxHandler<AmazonItemSearchSaxHandler.Element>  implements AmazonAlbumData {
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonItemSearchSaxHandler.class);
	
	enum Element {
		Item, ASIN,
		DetailPageURL,
		SmallImage,
		URL,
		Height,
		Width,
		MediumImage,
		LargeImage,
		ItemAttributes,
		Artist,
		Title,
		ImageSets,
		Error,
		Code,
		Message,
		IGNORED // anything we don't care about
	}

	private String artist;
	private String album;
	private String ASIN;
	private String productUrl;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	private String errorCode;
	private String errorMessage;

	AmazonItemSearchSaxHandler() {
		super(Element.class, Element.IGNORED);
		
		smallImageWidth = -1;
		smallImageHeight = -1;
	}

	AmazonItemSearchSaxHandler(String artist, String album) {
		this();
		this.artist = artist;
		this.album = album;
	}

	
	@Override
	protected void closeElement(Element c) throws SAXException {
		// there can be multiple items/images, we always use the first
		if (c == Element.ASIN) {
			if (ASIN == null)
				ASIN = getCurrentContent();
		} else if (c == Element.DetailPageURL) {
			if (productUrl == null)
				productUrl = getCurrentContent();
		} else if (parent() == Element.SmallImage) {
			if (c == Element.URL) {
				if (smallImageUrl == null)
					smallImageUrl = getCurrentContent();
			} else if (c == Element.Width) {
				String v = getCurrentContent();
				if (smallImageWidth < 0)
					smallImageWidth = Integer.parseInt(v);
			} else if (c == Element.Height) {
				String v = getCurrentContent();
				if (smallImageHeight < 0)
					smallImageHeight = Integer.parseInt(v);					
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
		//logger.debug("Parsed album results ASIN = " + ASIN + " smallImageUrl = " + smallImageUrl + " " + smallImageWidth + "x" + smallImageHeight);	
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
		return ASIN != null && smallImageUrl != null && 
		smallImageWidth > 0 && smallImageHeight > 0 && 
		productUrl != null;
	}
	
	public String getASIN() {
		return ASIN;
	}

	public String getProductUrl() {
		return productUrl;
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

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}
}
