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
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;

public class PostThumbnailServlet extends AbstractSmallImageServlet {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(PostThumbnailServlet.class);

	@Override
	protected void doUpload(HttpServletRequest request, HttpServletResponse response, User user,
			Map<String, String> params, FileItem photo) throws HttpException, IOException, ServletException, HumanVisibleException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("couldn't get sha1 algorithm", e);
		}
		byte[] sum = digest.digest(photo.get());
		String filename = StringUtils.hexEncode(sum);
		//logger.debug("computed sha1={} for uploaded object", filename);
		BufferedImage image = readPhoto(photo);
		
		/* FIXME there's no check on the size of this image, we're just 
		 * trusting the client, but the UI all assumes it's thumbnail-size
		 */
		
		writePhoto(image, filename, true);
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("rsp", "stat", "ok");
		xml.appendTextNode("url", "/files" + getRelativePath() + "/" + filename); 
		response.setContentType("text/xml; charset=UTF-8");
		response.getOutputStream().write(xml.getBytes());
	}

	@Override
	protected String getRelativePath() {
		return Configuration.POSTINFO_RELATIVE_PATH;
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
		return false;
	}
}
