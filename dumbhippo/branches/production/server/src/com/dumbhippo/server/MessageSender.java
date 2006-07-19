package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface MessageSender {
	public void sendPostNotification(Resource recipient, Post post, PostType postType);
	
	public void sendLivePostChanged(LivePost lpost, Guid excludeId);
	
	public void sendPostViewChanged(UserViewpoint viewpoint, Post post);	
	
	public void sendMySpaceNameChangedNotification(User user);
	
	public void sendMySpaceContactCommentNotification(User user);

	public void sendHotnessChanged(LiveUser user);

	public void sendActivePostsChanged(LiveUser user);
	
	public void sendPrefChanged(User user, String key, String value);

	public void sendGroupMembershipUpdate(Group group, GroupMember groupMember);
}
