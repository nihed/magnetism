package com.dumbhippo.postinfo;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.services.FlickrPhotosetData;

public class FlickrPostInfo extends PostInfo implements FlickrPhotosetData {

	FlickrPostInfo() {
		super(null, PostInfoType.FLICKR);
	}

	public List<PhotoData> getPhotoData() {
		List<PhotoData> ret = new ArrayList<PhotoData>();
		for (Node child : getTree().getChildren(NodeName.flickr, NodeName.photos)) {
				PhotoData data = new PhotoData();
				data.setThumbnailUrl(child.getContent(NodeName.photoUrl));
				try {				
					data.setPageUrl(child.getContent(NodeName.photoPageUrl));				
				} catch (NoSuchNodeException e) {
					// Ancient metadata version
				}
				ret.add(data);					
		}
		return ret;				
	}
}
