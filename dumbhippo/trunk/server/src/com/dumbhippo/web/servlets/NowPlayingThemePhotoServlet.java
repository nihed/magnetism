package com.dumbhippo.web.servlets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.dumbhippo.StringUtils;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.NowPlayingThemeSystem;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.web.WebEJBUtil;

public class NowPlayingThemePhotoServlet extends AbstractPhotoServlet {
	private static final long serialVersionUID = 1L;
	
	private NowPlayingThemeSystem nowPlayingSystem;
	
	@Override
	public void init() {
		super.init();
		nowPlayingSystem = WebEJBUtil.defaultLookup(NowPlayingThemeSystem.class);
	}	
		
	@Override
	protected void doUpload(HttpServletRequest request, HttpServletResponse response, User user,
			Map<String, String> formParameters, FileItem photo) throws HttpException, IOException, ServletException,
			HumanVisibleException {
		
		String mode = formParameters.get("mode");
		if (mode == null || !(mode.equals("active") || mode.equals("inactive")))
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "No mode= parameter or invalid value");
		
		String themeId = formParameters.get("theme");
		if (themeId == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "No theme= parameter");
		
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("couldn't get sha1 algorithm", e);
		}
		byte[] sum = digest.digest(photo.get());
		
		String hexSum = StringUtils.hexEncode(sum); 
		
		String filename = NowPlayingTheme.toFilename(mode, hexSum);

		BufferedImage image = readScaledPhoto(photo, Configuration.NOW_PLAYING_THEME_WIDTH, Configuration.NOW_PLAYING_THEME_HEIGHT); 
		
		writePhoto(image, filename, true);
		
		try {
			nowPlayingSystem.setNowPlayingThemeImage(new UserViewpoint(user), themeId, mode, hexSum);
		} catch (ParseException e) {
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Bad theme id", e);
		} catch (NotFoundException e) {
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Bad theme id", e);
		}
		
		doFinalRedirect(request, response);
	}

	@Override
	protected String getRelativePath() {
		return Configuration.NOW_PLAYING_THEMES_RELATIVE_PATH;
	}
	
	@Override
	protected String getDefaultFilename() {
		return null;
	}

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}

	@Override
	protected boolean getSaveJpegAlso() {
		return true;
	}
}
