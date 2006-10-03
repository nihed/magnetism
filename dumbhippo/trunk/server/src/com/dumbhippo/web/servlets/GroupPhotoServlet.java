package com.dumbhippo.web.servlets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.WebEJBUtil;

public class GroupPhotoServlet extends AbstractPhotoServlet {
	private static final long serialVersionUID = 1L;
	protected GroupSystem groupSystem;

	@Override
	public void init() {
		super.init();
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}

	@Override
	public String getRelativePath() {
		return Configuration.GROUPSHOTS_RELATIVE_PATH;
	}

	@Override
	protected void doUpload(HttpServletRequest request, HttpServletResponse response, User user,
			Map<String, String> formParameters, FileItem photo) throws HttpException, IOException, ServletException,
			HumanVisibleException {

		String groupId = formParameters.get("groupId");
		if (groupId == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "group ID not provided");

		// FIXME this will get cleaned up with future changes to have
		// doLogin return a viewpoint
		Viewpoint viewpoint = new UserViewpoint(user);
		Group group;
		try {
			group = groupSystem.lookupGroupById(viewpoint, groupId);
		} catch (NotFoundException e) {
			throw new HumanVisibleException("It looks like you can't change the photo for this group; maybe you are not in the group or there's no such group anymore?");
		}
		GroupMember member;
		try {
			member = groupSystem.getGroupMember(viewpoint, group, user);
		} catch (NotFoundException e) {
			member = null;
		}
		if (member == null || !member.canModify()) {
			throw new HumanVisibleException("You can't change the photo for a group unless you're in the group");
		}

		Collection<BufferedImage> scaled = readScaledPhotos(photo);
		writePhotos(scaled, groupId, true);
		
		groupSystem.incrementGroupVersion(group);
		
		// if we upload a photo we have to remove the stock photo that 
		// would otherwise override
		groupSystem.setStockPhoto(new UserViewpoint(user), group, null);
		
		doFinalRedirect(request, response);
	}

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return true;
	}

	@Override
	protected boolean getSaveJpegAlso() {
		return false;
	}
}
