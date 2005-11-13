package com.dumbhippo.web;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Viewpoint;

public class PhotoServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Log logger = GlobalSetup.getLog(PhotoServlet.class);

	private static final int PHOTO_DIMENSION = 48;
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 5; // 5M is huge, but photos can be big...
	// scaling something huge is probably bad, but allowing a typical desktop background is 
	// good if we can handle it...
	private static final int MAX_IMAGE_DIMENSION = 2048;
	
	private Configuration config;
	private GroupSystem groupSystem;
	private IdentitySpider identitySpider;
	private URI headshotSaveUri;
	private File headshotSaveDir;
	private URI groupshotSaveUri;
	private File groupshotSaveDir;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		
		String filesUrl = config.getPropertyFatalIfUnset(HippoProperty.FILES_SAVEURL);
		String headshotsUrl = filesUrl + Configuration.HEADSHOTS_RELATIVE_PATH;
		String groupshotsUrl = filesUrl + Configuration.GROUPSHOTS_RELATIVE_PATH;
		
		try {
			headshotSaveUri = new URI(headshotsUrl);
			groupshotSaveUri = new URI(groupshotsUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException("save url busted", e);
		}
		headshotSaveDir = new File(headshotSaveUri);
		groupshotSaveDir = new File(groupshotSaveUri);
	}
	
	private void doPhoto(HttpServletRequest request, HttpServletResponse response, boolean isGroup, DiskFileUpload upload, List items)
			throws HttpException, IOException, ServletException, ErrorPageException {

		FileItem photo = null;
		String groupId = null;
		
		File saveDir;
		String location;
		
		if (isGroup) {
			saveDir = groupshotSaveDir;
			location = Configuration.GROUPSHOTS_RELATIVE_PATH;
		} else {
			saveDir = headshotSaveDir;
			location = Configuration.HEADSHOTS_RELATIVE_PATH;
		}
		
		logger.debug("uploading photo to " + saveDir);

		Person user = doLogin(request, response, true);
		if (user == null)
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You must be logged in to change a photo");

		for (Object o : items) {
			FileItem item = (FileItem) o;
			if (item.isFormField()) {
				logger.debug("Form field " + item.getFieldName() + " = " + item.getString());
				if (item.getFieldName().equals("groupId")) {
					groupId = item.getString();
				}
			} else {
				photo = item;
			}
		}

		if (photo == null)
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "No photo uploaded?");
		
		String groupName = "";
		if (isGroup) {
			if (groupId == null)
				throw new HttpException(HttpResponseCode.BAD_REQUEST, "group ID not provided");
			
			// FIXME this will get cleaned up with future changes to have doLogin return a viewpoint/user thingy
			User u = identitySpider.getUser(user);
			Viewpoint viewpoint = new Viewpoint(u);
			Group group = groupSystem.lookupGroupById(viewpoint, groupId);
			if (group == null) {
				throw new ErrorPageException("It looks like you can't change the photo for this group; maybe you are not in the group or there's no such group anymore?");
			}
			GroupMember member = groupSystem.getGroupMember(viewpoint, group, u);
			if (!member.canModify()) {
				throw new ErrorPageException("You can't change the photo for a group unless you're in the group");
			}
			groupName = group.getName();
		}
		
		logger.debug("File " + photo.getName() + " size " + photo.getSize() + " content-type "
				+ photo.getContentType());

		InputStream in = photo.getInputStream();

		BufferedImage image = ImageIO.read(in);
		if (image == null) {
			throw new ErrorPageException("Our computer can't load that photo; either it's too dumb, or possibly you uploaded a file that isn't a picture?");
		}
		if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
			throw new ErrorPageException("That photo is really huge, which blows our computer's mind. Can you send us a smaller one?");
		}

		// FIXME It would be nicer to only pad images so they are always square and our 
		// css won't mangle their aspect ratio. I think to do this we have to manually
		// create the destination BufferedImage and then transform onto it.
		
		// FIXME it would be nicer to avoid round-tripping a jpeg through a BufferedImage

		double scaleX, scaleY;
		if (image.getHeight() > image.getWidth()) {
			scaleY = PHOTO_DIMENSION / (double) image.getHeight();
			scaleX = scaleY;
		} else {
			scaleX = PHOTO_DIMENSION / (double) image.getWidth();
			scaleY = scaleX;
		}

		logger.debug("Scaling photo scaleX = " + scaleX + " scaleY = " + scaleY + "new width = "
				+ image.getWidth() * scaleX + " new height = " + image.getHeight() * scaleY);

		AffineTransform tx = new AffineTransform();
		tx.scale(scaleX, scaleY);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		BufferedImage scaled = op.filter(image, null);
		File saveDest = new File(saveDir, isGroup ? groupId : user.getId());
		logger.debug("saving to " + saveDest.getCanonicalPath());

		// FIXME this should be JPEG, but Java appears to fuck that up
		// on a lot of images and produce a corrupt image
		// (someone on the internet says it's because scaling other than 
		// NEAREST_NEIGHBOR puts an alpha channel in the BufferedImage, 
		// which Java tries to save in the JPEG confusing most apps but 
		// not Java's own JPEG loader - see link on wiki)
		if (!ImageIO.write(scaled, "png", saveDest)) {
			throw new ErrorPageException("For some reason our computer couldn't save your photo. It's our fault; trying again later might help. If not, please let us know.");
		}
		
		request.setAttribute("photoLocation", location);
		request.setAttribute("photoFilename", isGroup ? groupId : user.getId());
		XmlBuilder link = new XmlBuilder();
		if (isGroup)
			link.appendTextNode("a", "Go to " + groupName, "href", "/viewgroup?groupId=" + groupId);
		else
			link.appendTextNode("a", "Go to your page", "href", "/home");
		request.setAttribute("homePageLink", link.toString());
		request.getRequestDispatcher("/newphoto").forward(request, response);
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, ServletException, ErrorPageException {
		DiskFileUpload upload = new DiskFileUpload();
		upload.setSizeMax(MAX_FILE_SIZE);
		
		try {
			List items = upload.parseRequest(request);

			if (request.getRequestURI().equals("/upload" + Configuration.HEADSHOTS_RELATIVE_PATH)) {
				doPhoto(request, response, false, upload, items);
			} else if (request.getRequestURI().equals("/upload" + Configuration.GROUPSHOTS_RELATIVE_PATH)) {
				doPhoto(request, response, true, upload, items);
			} else {
				throw new HttpException(HttpResponseCode.NOT_FOUND, "No upload page " + request.getRequestURI());
			}
			
		} catch (FileUploadException e) {
			// I don't have any real clue what this exception might be or indicate
			logger.error("File upload exception", e);
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "file upload malformed somehow; we aren't sure what went wrong");
		}
	}

	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException {
		logger.debug("PhotoServlet doesn't do anything on GET");
	}	
}
