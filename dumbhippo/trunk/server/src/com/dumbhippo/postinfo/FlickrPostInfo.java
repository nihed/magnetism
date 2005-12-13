package com.dumbhippo.postinfo;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.services.FlickrPhotosetData;

public class FlickrPostInfo extends PostInfo implements FlickrPhotosetData {

	FlickrPostInfo() {
		super(null, PostInfoType.FLICKR);
	}

	public List<String> getThumbnailUrls() {
		List<String> ret = new ArrayList<String>();
		for (Node child : getTree().getChildren(NodeName.flickr, NodeName.photos)) {
			ret.add(child.getContent(NodeName.photoUrl));
		}
		return ret;
	}
}
