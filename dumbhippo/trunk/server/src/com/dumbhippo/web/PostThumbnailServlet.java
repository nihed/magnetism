package com.dumbhippo.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;

public class PostThumbnailServlet extends AbstractSmallImageServlet {
	private static final long serialVersionUID = 1L;
	private static final Log logger = GlobalSetup.getLog(PostThumbnailServlet.class);

	@Override
	protected void doUpload(HttpServletRequest request, HttpServletResponse response, Person person, Map<String, String> params, FileItem photo) throws HttpException, IOException, ServletException, HumanVisibleException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("couldn't get sha1 algorithm", e);
		}
		byte[] sum = digest.digest(photo.get());
		String filename = StringUtils.hexEncode(sum);
		logger.debug("computed sha1=" + filename + " for uploaded object");
		BufferedImage image = readPhoto(photo);
		writePhoto(image, filename, true);
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("rsp", "stat", "ok");
		xml.appendTextNode("url", "/files" + getRelativePath() + "/" + filename); 
		response.setContentType("text/xml; charset=UTF-8");
		response.getOutputStream().write(xml.toString().getBytes("UTF-8"));
	}

	@Override
	protected String getRelativePath() {
		return Configuration.POSTINFO_RELATIVE_PATH;
	}
	
}
