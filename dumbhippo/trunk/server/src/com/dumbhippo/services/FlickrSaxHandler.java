package com.dumbhippo.services;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

public class FlickrSaxHandler extends EnumSaxHandler<FlickrSaxHandler.Element> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrSaxHandler.class);
	
	// The enum names should match the xml element names (including case).
	// indentation roughly matches xml nesting ;-)
	enum Element {
		rsp,
			err,
			user,
				username,
			photos,
				photo,
		IGNORED // an element we don't care about
	};	

	private boolean failed;
	private String errorMessage;
	private String errorCode;
	private String nsid;
	private String username;
	private FlickrUser user;
	private FlickrPhotos photos;
	
	FlickrSaxHandler() {
		super(Element.class, Element.IGNORED);
	}

	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.rsp) {
			String stat = currentAttributes().getValue("stat");
			if (stat != null && stat.equals("fail"))
				failed = true;
			else
				failed = false;
		} else if (c == Element.photos) {
			Attributes attrs = currentAttributes();
			photos = new FlickrPhotos();
			String page = attrs.getValue("page");
			String pages = attrs.getValue("pages");
			String perPage = attrs.getValue("perpage");
			String total = attrs.getValue("total");
			
			if (page == null || pages == null || perPage == null || total == null)
				throw new SAXException("Flickr photos element missing some attributes");
			try {
				photos.setPage(Integer.parseInt(page));
				photos.setPages(Integer.parseInt(pages));
				photos.setPerPage(Integer.parseInt(perPage));
				photos.setTotal(Integer.parseInt(total));
			} catch (NumberFormatException e) {
				throw new SAXException("Flickr photos element had malformed attribute value", e);
			}
		} else if (c == Element.username) {
			clearContent();
		}
	}
	
	private boolean parseFlickrBool(String value) throws SAXException {
		try {
			return Integer.parseInt(value) != 0;
		} catch (NumberFormatException e) {
			throw new SAXException("Flickr bool was not parseable as 0 or 1", e);
		}
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {
		if (failed) {
			if (c == Element.err) {
				errorMessage = currentAttributes().getValue("msg");
				errorCode = currentAttributes().getValue("code");
			}
		} else {
			if (c == Element.user) {
				nsid = currentAttributes().getValue("nsid");
			} else if (c == Element.username) {
				username = getCurrentContent();
			}  else if (c == Element.photo) {
				if (photos == null)
					throw new SAXException("Flickr photo element seen outside photos element");
				Attributes attrs = currentAttributes();
				FlickrPhoto photo = new FlickrPhoto();
				String id = attrs.getValue("id");
				String owner = attrs.getValue("owner");
				String secret = attrs.getValue("secret");
				String server = attrs.getValue("server");
				String title = attrs.getValue("title");
				String public_ = attrs.getValue("ispublic");
				String friend = attrs.getValue("isfriend");
				String family = attrs.getValue("isfamily");
				if (id == null || owner == null || secret == null || server == null ||
						title == null || public_ == null || friend == null || family == null) {
					throw new SAXException("missing needed attributes on Flickr photo element");
				}
				photo.setId(id);
				photo.setOwner(owner);
				photo.setSecret(secret);
				photo.setServer(server);
				photo.setTitle(title);
				photo.setPublic(parseFlickrBool(public_));
				photo.setFriend(parseFlickrBool(friend));
				photo.setFamily(parseFlickrBool(family));
				photos.addPhoto(photo);
			}
		}
	}

	@Override 
	public void endDocument() throws SAXException {
		if (failed)
			throw new ServiceException(false, "flickr error " + errorCode + ": " + errorMessage);
		
		if (nsid != null && username != null) {
			user = new FlickrUser();
			user.setId(nsid);
			user.setName(username);
		}
	}

	public FlickrUser getUser() {
		return user;
	}
	
	public FlickrPhotos getPhotos() {
		return photos;
	}
}
