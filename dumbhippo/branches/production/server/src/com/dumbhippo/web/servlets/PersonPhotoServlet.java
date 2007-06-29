package com.dumbhippo.web.servlets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.views.UserViewpoint;

public class PersonPhotoServlet extends AbstractPhotoServlet {
	private static final long serialVersionUID = 1L;
	
	@Override
	public void init() {
		super.init();
	}	
	
	@Override
	public String getRelativePath() { 
		return Configuration.HEADSHOTS_RELATIVE_PATH;
	}
	
	@Override
	protected void doUpload(HttpServletRequest request, HttpServletResponse response, User user,
			Map<String, String> formParameters, FileItem photo) throws HttpException, IOException, ServletException,
			HumanVisibleException {
		Collection<BufferedImage> scaled = readScaledPhotos(photo);
		String personId = user.getId();
		writePhotos(scaled, personId, true);
		
		identitySpider.incrementUserVersion(user);
		// The call to setStockPhoto also marks the change, but do it here in case we ever
		// make setStockPhoto short-circuit the null => null case.
		DataService.currentSessionRW().changed(UserDMO.class, user.getGuid(), "photoUrl");
		
		// if we upload a photo we have to remove the stock photo that 
		// would otherwise override
		identitySpider.setStockPhoto(new UserViewpoint(user), user, null);
		
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
