package com.dumbhippo.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.Viewpoint;

public class GroupPhotoServlet extends AbstractPhotoServlet {
	private static final long serialVersionUID = 1L;
	protected GroupSystem groupSystem;

	@Override
	public void init() {
		super.init();
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}

	public String getRelativePath() {
		return Configuration.GROUPSHOTS_RELATIVE_PATH;
	}

	protected void doUpload(HttpServletRequest request, HttpServletResponse response, Person person,
			Map<String, String> formParameters, FileItem photo) throws HttpException, IOException, ServletException,
			HumanVisibleException {

		String groupName = "";
		String groupId = formParameters.get("groupId");
		if (groupId == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "group ID not provided");

		// FIXME this will get cleaned up with future changes to have
		// doLogin return a viewpoint/user thingy
		User u = identitySpider.getUser(person);
		Viewpoint viewpoint = new Viewpoint(u);
		Group group = groupSystem.lookupGroupById(viewpoint, groupId);
		if (group == null) {
			throw new HumanVisibleException("It looks like you can't change the photo for this group; maybe you are not in the group or there's no such group anymore?");
		}
		GroupMember member = groupSystem.getGroupMember(viewpoint, group, u);
		if (!member.canModify()) {
			throw new HumanVisibleException("You can't change the photo for a group unless you're in the group");
		}
		groupName = group.getName();
		BufferedImage scaled = readScaledPhoto(photo);
		String filename = groupId;
		writePhoto(scaled, filename, true);
		doFinalRedirect(request, response, filename, "Go to group " + groupName, "/viewgroup?groupId=" + groupId);
	}
}
