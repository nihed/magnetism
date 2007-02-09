package com.dumbhippo.postinfo;

public class ShareGroupPostInfo extends PostInfo {

	ShareGroupPostInfo() {
		super(null, PostInfoType.SHARE_GROUP);
	}

	public String getGroupId() {
		return getTree().getContent(NodeName.shareGroup, NodeName.groupId);
	}
}
