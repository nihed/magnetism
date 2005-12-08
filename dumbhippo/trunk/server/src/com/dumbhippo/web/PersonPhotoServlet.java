package com.dumbhippo.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;

public class PersonPhotoServlet extends AbstractPhotoServlet {
	private static final long serialVersionUID = 1L;
	public String getRelativePath() { 
		return Configuration.HEADSHOTS_RELATIVE_PATH;
	}
	protected void doUpload(HttpServletRequest request, HttpServletResponse response, Person person,
			Map<String, String> formParameters, FileItem photo) throws HttpException, IOException, ServletException,
			HumanVisibleException {
		BufferedImage scaled = readScaledPhoto(photo);
		String filename = person.getId();
		writePhoto(scaled, filename, true);
		doFinalRedirect(request, response, filename, "Go to your page", "/home");
	}
}
