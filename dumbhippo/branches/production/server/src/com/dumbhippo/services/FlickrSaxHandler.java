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
			photosets,
				photoset,
					title,
					description,
		IGNORED // an element we don't care about
	};	

	private boolean failed;
	private String errorMessage;
	private String errorCode;
	private String nsid;
	private String username;
	private FlickrUser user;
	private FlickrPhotos photos;
	private FlickrPhotosets photoSets;
	private FlickrPhotoset photoSet; 
	
	FlickrSaxHandler() {
		super(Element.class, Element.IGNORED);
	}

	private FlickrPhoto newPhoto(String id, String secret, String server) {
		FlickrPhoto photo = new FlickrPhoto();
		photo.setId(id);
		photo.setSecret(secret);
		photo.setServer(server);
		
		return photo;
	}
	
	// can take a <photos> element's attrs or a <photoset> element's attrs when <photoset> is a root node
	private FlickrPhotos newPhotos(Attributes attrs) throws SAXException {
		FlickrPhotos photos = new FlickrPhotos();
		String page = attrs.getValue("page");
		String pages = attrs.getValue("pages");
		String perPage = attrs.getValue("perpage");
		if (perPage == null)
			perPage = attrs.getValue("per_page"); // this is in the docs, but maybe not in real life?
		String total = attrs.getValue("total");
		
		if (page == null || pages == null || perPage == null || total == null)
			throw new SAXException("Flickr photos element missing some attributes " + attrs);
		try {
			photos.setPage(Integer.parseInt(page));
			photos.setPages(Integer.parseInt(pages));
			photos.setPerPage(Integer.parseInt(perPage));
			photos.setTotal(Integer.parseInt(total));
		} catch (NumberFormatException e) {
			throw new SAXException("Flickr photos element had malformed attribute value", e);
		}
		
		return photos;
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
			if (photos != null)
				throw new SAXException("two <photos> elements in response");
			
			Attributes attrs = currentAttributes();
			photos = newPhotos(attrs);
		} else if (c == Element.photosets) {
			if (photoSets != null)
				throw new SAXException("two <photosets> elements in response");
			
			photoSets = new FlickrPhotosets();
		} else if (c == Element.photoset) {
			
			FlickrPhotoset set = new FlickrPhotoset();
			
			if (photoSets != null) {
				// <photosets> is our parent when listing available photosets				
				Attributes attrs = currentAttributes();
				String id = attrs.getValue("id");
				String primary = attrs.getValue("primary");
				String secret = attrs.getValue("secret"); // optional
				String server = attrs.getValue("server");
				String photos = attrs.getValue("photos");
				
				if (id == null || primary == null || server == null || photos == null) {
					throw new SAXException("missing needed attributes on <photoset> " + attrs);
				}
				
				FlickrPhoto primaryPhoto = newPhoto(primary, secret, server);
				primaryPhoto.setPrimary(true);
				set.setPrimaryPhoto(primaryPhoto);
				
				set.setId(id);
				try {
					set.setPhotoCount(Integer.parseInt(photos));
				} catch (NumberFormatException e) {
					throw new SAXException("Count of photos in photoset is not a number?", e);
				}
				
				photoSets.addSet(set);
			} else {
				// <photoset> is the root element when listing a photoset contents
				Attributes attrs = currentAttributes();
				String id = attrs.getValue("id");
				FlickrPhotos photos = newPhotos(attrs);
				
				if (id == null) {
					throw new SAXException("Flickr <photoset> element missing some attributes " + attrs);
				} else {
					set.setId(id);
				
					set.setPhotos(photos);

					set.setPhotoCount(photos.getTotal());
				}
			}
			
			photoSet = set;
		
		} else if (c == Element.username || c == Element.title || c == Element.description) {
			// drop any content prior to the open tag
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
				if (photoSet != null) {
					Attributes attrs = currentAttributes();
					
					String id = attrs.getValue("id");
					String secret = attrs.getValue("secret");
					String server = attrs.getValue("server");
					String title = attrs.getValue("title");
					String primary = attrs.getValue("isprimary");
					
					if (id == null || secret == null || server == null || title == null || primary == null) {
						throw new SAXException("missing needed attributes on Flickr photo inside photoset");
					}
					FlickrPhoto photo = newPhoto(id, secret, server);
					photo.setPrimary(parseFlickrBool(primary));
					photo.setTitle(title);
					
					photoSet.getPhotos().addPhoto(photo);
				} else {
					if (photos == null)
						throw new SAXException("Flickr photo element seen outside photos element");
					Attributes attrs = currentAttributes();
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
					
					FlickrPhoto photo = newPhoto(id, secret, server);
					
					photo.setOwner(owner);
					photo.setTitle(title);
					photo.setPublic(parseFlickrBool(public_));
					photo.setFriend(parseFlickrBool(friend));
					photo.setFamily(parseFlickrBool(family));
					photos.addPhoto(photo);
				}
			} else if (c == Element.title) {
				if (photoSet == null)
					throw new SAXException("<title> should be inside <photoset>");
				photoSet.setTitle(getCurrentContent());
			} else if (c == Element.description) {
				if (photoSet == null)
					throw new SAXException("<description> should be inside <photoset>");
				photoSet.setDescription(getCurrentContent());
			} else if (c == Element.photoset) {
				// when doing a <photosets> we have a series of photoset, when 
				// doing a listing of a photoset we just have one photoset so 
				// we leave it available
				if (photoSets != null)
					photoSet = null;
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
	
	public FlickrPhotosets getPhotosets() {
		return photoSets;
	}
	
	public FlickrPhotoset getPhotoset() {
		return photoSet;
	}
}
