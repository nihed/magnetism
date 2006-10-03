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
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.WebEJBUtil;

public class PersonPhotoServlet extends AbstractPhotoServlet {
	private static final long serialVersionUID = 1L;
	
	private IdentitySpider identitySpider;
	
	@Override
	public void init() {
		super.init();
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
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
