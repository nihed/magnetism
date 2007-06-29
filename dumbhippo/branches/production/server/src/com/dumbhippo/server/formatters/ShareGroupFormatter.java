package com.dumbhippo.server.formatters;

import java.util.Set;

import javax.ejb.EJBContext;

import org.slf4j.Logger;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.ShareGroupPostInfo;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

public class ShareGroupFormatter extends DefaultFormatter {

	static private final Logger logger = GlobalSetup.getLogger(ShareGroupFormatter.class);
	
	private String inviterName;
	
	@Override
	public void init(PostView postView, EJBContext ejbContext) {
		super.init(postView, ejbContext);

		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return;
		
		ShareGroupPostInfo shareGroupInfo = (ShareGroupPostInfo) postInfo;
		
		String groupId = shareGroupInfo.getGroupId();
		
		GroupSystem groupSystem = EJBUtil.contextLookup(ejbContext, GroupSystem.class);
		
		Group group = null;
		GroupMember member = null;
		try {
			Viewpoint viewpoint = postView.getViewpoint();
			
			group = groupSystem.lookupGroupById(viewpoint, groupId);
			
			if (postView.getContext() == PostView.Context.MAIL_NOTIFICATION)
				member = groupSystem.getGroupMember(group, postView.getMailRecipient());
			else {
				if (viewpoint instanceof UserViewpoint)
					member = groupSystem.getGroupMember(postView.getViewpoint(), group, ((UserViewpoint)viewpoint).getViewer());
			}
		} catch (NotFoundException e) {
			// this is just debug() not warn() since a recipient can leave a group, which will mean no GroupMember, which is normal
			logger.debug("Bad group ID {} in post {} or recipient has no GroupMember", groupId, postView.getPost().getId());
			logger.debug("NotFoundException on group id: {}", e.getMessage());
		}
		
		if (member != null) {
			Set<User> adders = member.getAdders();
			// adders will be empty if you created the group
			// TODO: reflect all adders in inviterName(s)
			if (adders.iterator().hasNext()) {
				String nick = adders.iterator().next().getNickname();
				if (nick != null) {
					FullName parsed = FullName.parseHumanString(nick);
					inviterName = parsed.getFirstName();
				}
			}
		}
	}
	
	@Override
	public String getTextAsText() {
		StringBuilder sb = new StringBuilder();
		if (inviterName != null) {
			sb.append("        ");
			sb.append(inviterName);
			sb.append(" invited you to join this group.\n");
		}
		sb.append(super.getTextAsText());
		return sb.toString();
	}
	
	@Override
	public String getTextAsHtml() {		
		XmlBuilder xml = new XmlBuilder();
		
		xml.append("<div>");
		if (inviterName != null) {
			xml.append("<div><span style=\"width: 3em;\"></span><em>");
			xml.appendEscaped(inviterName);
			xml.appendEscaped(" invited you to join this group.");
			xml.append("</em>");
			xml.append("</div>");
		}
		
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		xml.append("</div>");
		
		return xml.toString();
	}
}
