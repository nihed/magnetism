package com.dumbhippo.services;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;


public class AmazonItemSearch extends
		AbstractXmlRequest<AmazonItemSearchSaxHandler> {

	static private final Logger logger = GlobalSetup.getLogger(AmazonItemSearch.class);
	
	public AmazonItemSearch(int timeoutMilliseconds) {
		super(timeoutMilliseconds);
	}

	public AmazonAlbumData searchAlbum(String amazonAccessKeyId, String amazonAssociateTag, String artist, String albumTitle) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService"
				+ "&Operation=ItemSearch&MerchantId=All&Condition=All&SearchIndex=Music"
				+ "&ResponseGroup=Small,Images&AWSAccessKeyId=");
		sb.append(amazonAccessKeyId);
		if (amazonAssociateTag != null) {
			sb.append("&AssociateTag=");
			sb.append(amazonAssociateTag);
		}
		sb.append("&Artist=");
		sb.append(StringUtils.urlEncode(artist));		
		sb.append("&Title=");
		sb.append(StringUtils.urlEncode(albumTitle));		

		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		
		AmazonItemSearchSaxHandler handler = parseUrl(new AmazonItemSearchSaxHandler(artist, albumTitle), wsUrl);
		
		return handler;
	}


	public static void main(String[] args) {
		AmazonItemSearch itemSearch = new AmazonItemSearch(10000);
		AmazonAlbumData album = itemSearch.searchAlbum("", "",
				"Wilco", "Summerteeth");
		if (album == null)
			logger.debug("Failed to get a result");
		else
			logger.debug("ASIN " + album.getASIN() + " product page " + album.getProductUrl() +
					" image link " + album.getSmallImageUrl() + " " + album.getSmallImageWidth() + "x" + album.getSmallImageHeight());
	}
}

