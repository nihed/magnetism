package com.dumbhippo.postinfo;

import com.dumbhippo.services.AmazonItemView;

public class AmazonPostInfo extends PostInfo implements AmazonItemView {

	AmazonPostInfo() {
		super(null, PostInfoType.AMAZON);
	}

	public void merge(AmazonItemView itemData) {
		Node amazon = getTree().resolveOrCreatePath(NodeName.amazon);
		amazon.updateContentChild(itemData.getItemId(), NodeName.itemId);
		amazon.updateContentChild(itemData.getTitle(), NodeName.title);
		amazon.updateContentChild(itemData.getEditorialReview(), NodeName.editorialReview);
		amazon.updateContentChild(itemData.getNewPrice(), NodeName.newPrice);
		amazon.updateContentChild(itemData.getUsedPrice(), NodeName.usedPrice);
		amazon.updateContentChild(itemData.getRefurbishedPrice(), NodeName.refurbishedPrice);
		amazon.updateContentChild(itemData.getCollectiblePrice(), NodeName.collectiblePrice);
		String smallImageUrl = itemData.getSmallImageUrl();
		if (smallImageUrl != null) {
			amazon.resolveOrCreatePath(NodeName.smallPhoto, NodeName.url).setContent(smallImageUrl);
			amazon.resolveOrCreatePath(NodeName.smallPhoto, NodeName.width).setInteger(itemData.getSmallImageWidth());
			amazon.resolveOrCreatePath(NodeName.smallPhoto, NodeName.height).setInteger(itemData.getSmallImageHeight());
		} else {
			amazon.removeChildIfExists(NodeName.smallPhoto);
		}
		// drop the amazon node if it's empty anyway
		getTree().removeChildIfNoChildren(NodeName.amazon);
	}
	
	public String getItemId() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.itemId);
	}

	public String getTitle() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.title);
	}
	
	public String getEditorialReview() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.editorialReview);
	}
	
	public String getNewPrice() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.newPrice);
	}

	public String getUsedPrice() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.usedPrice);
	}

	public String getCollectiblePrice() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.collectiblePrice);
	}

	public String getRefurbishedPrice() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.refurbishedPrice);
	}

	public String getSmallImageUrl() {
		return getTree().getContentIfExists(NodeName.amazon, NodeName.smallPhoto, NodeName.url);
	}

	public int getSmallImageWidth() {
		try {
			return getTree().getInteger(NodeName.amazon, NodeName.smallPhoto, NodeName.width);
		} catch (NoSuchNodeException e) {
			return 32; // why not
		}
	}

	public int getSmallImageHeight() {
		try {
			return getTree().getInteger(NodeName.amazon, NodeName.smallPhoto, NodeName.height);
		} catch (NoSuchNodeException e) {
			return 32; // why not
		}
	}

}
