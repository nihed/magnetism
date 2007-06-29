package com.dumbhippo.postinfo;

import com.dumbhippo.services.EbayItemData;

public class EbayPostInfo extends PostInfo implements EbayItemData {

	EbayPostInfo() {
		super(null, PostInfoType.EBAY);
	}
	
	public void merge(EbayItemData itemData) {
		Node ebay = getTree().resolveOrCreatePath(NodeName.eBay);
		
		String smallImageUrl = itemData.getPictureUrl();
		if (smallImageUrl != null) {
			ebay.resolveOrCreatePath(NodeName.smallPhoto, NodeName.url).setContent(smallImageUrl);
			// width/height are unknown
			ebay.removeChildIfExists(NodeName.smallPhoto, NodeName.width);
			ebay.removeChildIfExists(NodeName.smallPhoto, NodeName.height);
		} else {
			ebay.removeChildIfExists(NodeName.smallPhoto);
		}
		
		ebay.updateContentChild(itemData.getTimeLeft(), NodeName.timeLeft);
		ebay.updateContentChild(itemData.getBuyItNowPrice(), NodeName.buyItNowPrice);
		ebay.updateContentChild(itemData.getStartPrice(), NodeName.startPrice);
		
		// drop the ebay node if it's empty anyway
		getTree().removeChildIfNoChildren(NodeName.eBay);
	}
	
	public String getPictureUrl() {
		return getTree().getContentIfExists(NodeName.eBay, NodeName.smallPhoto, NodeName.url);
	}

	public String getTimeLeft() {
		return getTree().getContentIfExists(NodeName.eBay, NodeName.timeLeft);
	}

	public String getBuyItNowPrice() {
		return getTree().getContentIfExists(NodeName.eBay, NodeName.buyItNowPrice);
	}

	public String getStartPrice() {
		return getTree().getContentIfExists(NodeName.eBay, NodeName.startPrice);
	}
}
