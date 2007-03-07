package com.dumbhippo.web.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.applications.AppinfoFile;
import com.dumbhippo.server.applications.AppinfoIcon;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.web.WebEJBUtil;

/**
 * This servlet handles various operations related to retrieving information from and
 * updating the application database.
 * 
 * @author otaylor
 */
public class AppinfoServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(AppinfoServlet.class);
	
	// Maximum size of a file upload we keep in memory
	private static final int MAX_IN_MEMORY_SIZE = 32 * 1024;
	
	// Maximum size for an uploaded appinfo file; current appinfo files 
	// range from about 8k to 64k
	private static final long MAX_APPINFO_FILE_SIZE = 256 * 1024;
	
	// Maximum size for a single uploaded icon; some SVG icons can
	// be around 50k or so.
	private static final long MAX_ICON_FILE_SIZE = 128 * 1024;
	
	private Configuration config;
	private ApplicationSystem applicationSystem;
	private IdentitySpider identitySpider;
	private File appinfoDir;

	@Override
	public void init() throws ServletException {
		super.init();
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		try {
			appinfoDir = new File(config.getPropertyNoDefault(HippoProperty.APPINFO_DIR));
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException("appinfoDir property was not set in super configuration");
		}
	}

	////////////////////////////////////////////////////////////
	//
	// PUT /upload/appinfo
	//
	// Upload a .dappinfo file
	
	protected String doUpload(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(MAX_IN_MEMORY_SIZE);
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		upload.setSizeMax(MAX_APPINFO_FILE_SIZE);
		
		User user = getUser(request);
		// isAdminstrator re-attaches
		if (user == null || !identitySpider.isAdministrator(user))
			throw new HttpException(HttpResponseCode.FORBIDDEN, "you don't have permission to upload application info files");
		
		Guid uploadId = Guid.createNew();
		
		File saveLocation = new File(appinfoDir, uploadId.toString() + ".dappinfo");
		
		Collection<?> items;
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			logger.warn("Error parsing appinfo file upload");
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed somehow; we aren't sure what went wrong");
		}
		
		for (Object o : items) {
			FileItem item = (FileItem) o;
			String name = item.getFieldName();
			
			if (name.equals("appinfo_file")) {
				try {
					item.write(saveLocation);
				} catch (Exception e) {
					logger.warn("Couldn't write appinfo dump to disk", e);
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "Couldn't save appinfo file");
				}
				
				break;
			}
		}
		
		AppinfoFile file;
		try {
			file = new AppinfoFile(saveLocation);
		} catch (IOException e) {
			saveLocation.delete();
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Error reading appinfo file: " + e.getMessage());
		} catch (ValidationException e) {
			saveLocation.delete();
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "appinfo file didn't validate: " + e.getMessage());
		}

		try {
			applicationSystem.addUpload(user.getGuid(), uploadId, file);
		} catch (RuntimeException e) {
			file.close();
			saveLocation.delete();
			throw e;
		}
		
		file.close();
		logger.debug("Appinfo file succesfully written to {}", saveLocation.getPath());
		
		return null;
	}
	
	////////////////////////////////////////////////////////////
	//
	// PUT /upload/appinfo-edit
	//
	// Form-post to edit a previously loaded appinfo file

	private static final Pattern ADD_ICON_NAME_REGEX = Pattern.compile("^icon([0-9]+)-(file|theme|size)$");
	private static final Pattern DELETE_ICON_NAME_REGEX = Pattern.compile("^delete-icon(?:\\.([A-Za-z0-9-_]+))?(?:\\.(\\d+x\\d+|scalable))?$");
	
	private static class AddIconSpec {
		String theme;
		String size;
		FileItem item;
		private String extension;

		public FileItem getItem() {
			return item;
		}

		public void setItem(FileItem item) {
			this.item = item;
		}

		public String getSize() {
			return size;
		}

		public void setSize(String size) {
			this.size = size;
		}

		public String getTheme() {
			return theme;
		}

		public void setTheme(String theme) {
			this.theme = theme;
		}

		public String getExtension() {
			return extension;
		}
		
		public void setExtension(String extension) {
			this.extension = extension;
		}
	}

	private static class DeleteIconSpec {
		private String theme;
		private String size;
		
		public DeleteIconSpec(String theme, String size) {
			this.theme = theme;
			this.size = size;
		}
		
		public String getSize() {
			return size;
		}
		public String getTheme() {
			return theme;
		}
	}
	
	private static class EditSpec {
		String appId = null;
		String name = null;
		String description = null;
		String wmClasses = null;
		String titlePatterns = null;
		String desktopNames = null;
		String categories = null;
		List<DeleteIconSpec> toDelete = new ArrayList<DeleteIconSpec>();
		Map<Integer, AddIconSpec> toAdd = new HashMap<Integer,AddIconSpec>();
		
		public String getAppId() {
			return appId;
		}

		public void readFormData(Collection<?> items) throws HttpException{
			for (Object o : items) {
				FileItem item = (FileItem)o; 
				String fieldName = item.getFieldName();
				
				if (fieldName.equals("appId")) {
					appId = item.getString();
				} else if (fieldName.equals("name")) {
					name = item.getString();
				} else if (fieldName.equals("description")) {
					description = item.getString();
				} else if (fieldName.equals("wmClasses")) {
					wmClasses = item.getString();
				} else if (fieldName.equals("titlePatterns")) {
					titlePatterns = item.getString();
				} else if (fieldName.equals("desktopNames")) {
					desktopNames =item.getString();
				} else if (fieldName.equals("categories")) {
					categories = item.getString();
				} else {
					Matcher m = ADD_ICON_NAME_REGEX.matcher(fieldName);
					if (m.matches()) {
						int index = Integer.parseInt(m.group(1));
						AddIconSpec addSpec = toAdd.get(index);
						if (addSpec == null) {
							addSpec = new AddIconSpec();
							toAdd.put(index, addSpec);
						}

						if ("file".equals(m.group(2))) {
							addSpec.setItem(item);

							if (item.getName() != null && item.getName().endsWith(".png") || item.getName().endsWith(".PNG"))
								addSpec.setExtension("png");
							else if (item.getName() != null && item.getName().endsWith(".svg") ||  item.getName().endsWith(".SVG"))
								addSpec.setExtension(".svg");
							else if (item.getName() != null && item.getName().endsWith(".svgz") ||  item.getName().endsWith(".SVGZ"))
								addSpec.setExtension(".svgz");
							else
								throw new HttpException(HttpResponseCode.BAD_REQUEST, "Can't determine image type from filename '" + item.getName() + "'");
							
						} else if ("theme".equals(m.group(2))) {
							addSpec.setTheme(item.getString());
							
						} else if ("size".equals(m.group(2))) {
							String size = item.getString();
							if ("unspecified".equals(size))
								size = null;
							
							addSpec.setSize(size);
						}
						
					} else {
						m = DELETE_ICON_NAME_REGEX.matcher(fieldName);
						if (m.matches()) {
							toDelete.add(new DeleteIconSpec(m.group(1), m.group(2)));
						}
					}
				}
			}
		}

		public void edit(AppinfoFile appinfoFile) throws ValidationException {
			appinfoFile.setName(name);
			appinfoFile.setDescription(description);
			appinfoFile.setCategoriesString(categories);
			appinfoFile.setWmClassesString(wmClasses);
			appinfoFile.setTitlePatternsString(titlePatterns);
			appinfoFile.setDesktopNamesString(desktopNames);
			
			for (DeleteIconSpec spec : toDelete)
				appinfoFile.deleteIcon(spec.getTheme(), spec.getSize());
			
			for (AddIconSpec spec : toAdd.values()) {
				if (spec != null && spec.getItem() != null) {
					appinfoFile.addIcon(spec.getTheme(), spec.getSize(), spec.getExtension(),
										spec.getItem().get());
				}
			}
		}
	}

	private String doEdit(HttpServletRequest request, HttpServletResponse response)  throws HttpException, IOException {
		boolean success = false;
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(MAX_IN_MEMORY_SIZE);
		
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		upload.setSizeMax(MAX_ICON_FILE_SIZE);
		
		User user = getUser(request);
		// isAdminstrator re-attaches
		if (user == null || !identitySpider.isAdministrator(user))
			throw new HttpException(HttpResponseCode.FORBIDDEN, "you don't have permission to upload application info files");
		
		Guid uploadId = Guid.createNew();
		
		File saveLocation = new File(appinfoDir, uploadId.toString() + ".dappinfo");
		
		Collection<?> items;
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			logger.warn("Error parsing appinfo file upload");
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed somehow; we aren't sure what went wrong", e);
		}
		
		EditSpec spec = new EditSpec();
		spec.readFormData(items);
		
		if (spec.getAppId() == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "appId not specified");

		AppinfoFile file = null;
		OutputStream out = null;
		
		try {
			try {
				file = applicationSystem.getAppinfoFile(spec.getAppId());
			} catch (NotFoundException e) {
				throw new HttpException(HttpResponseCode.BAD_REQUEST, "appId doesn't reference a known application");
			}
		
			try {
				spec.edit(file);
			} catch (ValidationException e) {
				throw new HttpException(HttpResponseCode.BAD_REQUEST, e.getMessage());
			}

			out = new FileOutputStream(saveLocation);
			file.write(out);
			out.close();
			out = null;
			

			applicationSystem.addUpload(user.getGuid(), uploadId, file);

			logger.debug("Edited appinfo file succesfully written to {}", saveLocation.getPath());
			success = true;
			
		} finally {
			if (file != null)
				file.close();
			
			if (out != null)
				out.close();

			if (!success)
				saveLocation.delete();
		}
		
		logger.debug("Appinfo file succesfully written to {}", saveLocation.getPath());

		response.sendRedirect("/application-edit?id=" + spec.getAppId());
		
		return null;
	}

	////////////////////////////////////////////////////////////
	//
	// GET /files/appinfo-icon/<appId>
	//
	// Retrieve an icon from a previously uploaded appinfo file

	private String doGetIcon(HttpServletRequest request, HttpServletResponse response)  throws HttpException, IOException {
		String applicationId = request.getPathInfo().substring(1);
		String theme = request.getParameter("theme");
		if ("generic".equals(theme))
			theme = null;
		
		String size = request.getParameter("size");
		
		AppinfoFile appinfoFile;
		
		try {
			 appinfoFile = applicationSystem.getAppinfoFile(applicationId);
		} catch (NotFoundException e) {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Application not found");
		}
		
		try {
			for (AppinfoIcon icon : appinfoFile.getIcons()) {
				if (!icon.matches(theme, size))
					continue;
				
				if (icon.getPath().endsWith(".png"))
					response.setContentType("image/png");
				else if (icon.getPath().endsWith(".svg") || icon.getPath().endsWith(".svgz"))
					response.setContentType("image/svg+xml");
				else
					throw new HttpException(HttpResponseCode.NOT_FOUND, "Application icon has unknown type");
				
				InputStream in = appinfoFile.getIconStream(icon);
				OutputStream out = response.getOutputStream();
				StreamUtils.copy(in, out);
				
				return null;
			}
		
			throw new HttpException(HttpResponseCode.NOT_FOUND, "Icon not found");
		} finally {
			appinfoFile.close();
		}
	}
	
	////////////////////////////////////////////////////////////

	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		if (request.getServletPath().equals("/upload/appinfo"))
			return doUpload(request, response);
		if (request.getServletPath().equals("/upload/appinfo-edit"))
			return doEdit(request, response);
		else
			throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "AppinfoServlet called for bad path");
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		if (request.getServletPath().equals("/files/appinfo-icon"))
			return doGetIcon(request, response);
		else
			throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "AppinfoServlet called for bad path");
	}
	
	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		// We don't want to hold a transaction while processing the upload or icon download
		return false;
	}
}
