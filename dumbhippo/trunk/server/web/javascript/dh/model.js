// This file contains the "model" objects, like Person
dojo.provide("dh.model");
dojo.require("dh.util");
dojo.require("dh.lang");
dojo.require("dh.dom");

dh.model.Track = function(image, title, artist, album, stillPlaying) {
	this.image = image;
	this.title = title;
	this.artist = artist;
	this.album = album;
	this.stillPlaying = stillPlaying;
}

dh.model.trackFromXmlNode = function(element) {
	if (element.nodeName != "song")
		dh.raise("not a song element");
	var image = null;
	var title = null;
	var artist = null;
	var album = null;
	var stillPlaying = null;
	
	var i;
	for (i = 0; i < element.childNodes.length; ++i) {
		var n = element.childNodes.item(i);
		if (n.nodeName == "title")
			title = dh.dom.textContent(n);
		else if (n.nodeName == "image")
			image = dh.dom.textContent(n);
		else if (n.nodeName == "artist")
			artist = dh.dom.textContent(n);
		else if (n.nodeName == "album")
			album = dh.dom.textContent(n);
		else if (n.nodeName == "stillPlaying");
			stillPlaying = dh.dom.textContent(n);
	}
	
	return new dh.model.Track(image, title, artist, album, stillPlaying == "true");
}

dh.model.UpdateItem = function(title, link, text, timestamp, photos) {
	this.title = title;
	this.link = link;
	this.text = text;
	this.timestamp = timestamp;
	this.photos = photos;
}

dh.model.updateItemFromXmlNode = function(element) {		
    if (element.nodeName != "updateItem")
        dh.raise("updateItem node expected");		
			
	var title = null;
	var link = null;
	var text = null;
	var timestamp = null;
	var photos = [];
	    			
	itemChildNodes = element.childNodes;	
		
	var updateTitleNode = itemChildNodes.item(0);
	if (updateTitleNode.nodeName != "updateTitle")
		dh.raise("updateTitle node expected");
	title = dh.dom.textContent(updateTitleNode);

	var updateLinkNode = itemChildNodes.item(1);
	if (updateLinkNode.nodeName != "updateLink")
		dh.raise("updateLink node expected");
	link = dh.dom.textContent(updateLinkNode);
				
	var updateTextNode = itemChildNodes.item(2);
	if (updateTextNode.nodeName != "updateText")
		dh.raise("updateText node expected");
	text = dh.dom.textContent(updateTextNode);
	
	// "updateTimestamp" is optional
	if (itemChildNodes.length > 3) {
	    var updateTimestampNode = itemChildNodes.item(3);
	    if (updateTimestampNode.nodeName != "updateTimestamp")
		    dh.raise("updateTimestamp node expected");
	    timestampString = dh.dom.textContent(updateTimestampNode);	    	
	    timestamp = parseInt(timestampString);
	    if (timestamp == NaN)
		    dh.raise("failed to parse '" + timestampString + "' as an integer for a timestamp");
	}
	
	if (itemChildNodes.length > 4) {
	    var updatePhotosNode = itemChildNodes.item(4);
	    if (updatePhotosNode.nodeName != "updatePhotos")
		    dh.raise("updatePhotos node expected");
	    
	    photoNodes = updatePhotosNode.childNodes;
		var i;
		for (i = 0; i < photoNodes.length; ++i) {
			var photoNode = photoNodes.item(i);
			var photo = dh.model.updatePhotoDataFromXmlNode(photoNode);
			photos.push(photo);
		}
	    	    
	}	    	    
	
	return new dh.model.UpdateItem(title, link, text, timestamp, photos);
}

dh.model.PhotoData = function(link, source, caption) {
	this.link = link;
	this.source = source;
	this.caption = caption;
}

dh.model.updatePhotoDataFromXmlNode = function(element) {		
    if (element.nodeName != "photo")
        dh.raise("photo node expected");		
			
	var link = null;
	var source = null;
	var caption = null;
	    			
	photoChildNodes = element.childNodes;	
		
	var linkNode = photoChildNodes.item(0);
	if (linkNode.nodeName != "photoLink")
		dh.raise("photoLink node expected");
	link = dh.dom.textContent(linkNode);

	var sourceNode = photoChildNodes.item(1);
	if (sourceNode.nodeName != "photoSource")
		dh.raise("photoSource node expected");
	source = dh.dom.textContent(sourceNode);
	
    var captionNode = photoChildNodes.item(2);
	if (captionNode.nodeName != "photoCaption")
		dh.raise("photoCaption node expected");
	caption = dh.dom.textContent(captionNode);
	
	return new dh.model.PhotoData(link, source, caption);
}	
	
dh.model.Message = function(text, fromId, fromNickname, serial, timestamp) {
	this.text = text;
	this.fromId = fromId;
	this.fromNickname = fromNickname;
	this.serial = serial;
	this.timestamp = timestamp;
}

dh.model.messageFromXmlNode = function(element) {
	if (element.nodeName != "message")
		dh.raise("not a message element");
	var text = dh.dom.textContent(element);
	var from = element.getAttribute("fromId");
	var nick = element.getAttribute("fromNickname");
	var serial = element.getAttribute("serial");	
	var timestamp = element.getAttribute("timestamp");

	return new dh.model.Message(text, from, nick ,parseInt(serial), parseInt(timestamp));
}

dh.model.GuidPersistable = function(id, displayName) {
	this.id = id;
	this.displayName = displayName;
	this.kind = null;
	
	this.isPerson = function() { 
		return this.kind == "person";
	}
	
	this.isGroup = function() {
		return this.kind == "group";
	}
	
	this.isTheWorld = function() {
		return this.kind == "world";
	}
}
dh.inherits(dh.model.GuidPersistable, Object);

dh.model.Person = function(id, userId, displayName, email, aim, emails, aims, hasAccount, photoUrl, homeUrl) {
	this.id = id;
	if (userId == "")
		userId = null;
	this.userId = userId;	
	this.displayName = displayName;
	this.email = email; // can be null
	this.aim = aim; // can be null
	this.emails = emails ? emails : []; // array of string, can be empty
	this.aims = aims ? aims : []; // array of string, can be empty
	this.hasAccount = hasAccount;
	this.photoUrl = photoUrl;
	this.homeUrl = homeUrl;
	this.kind = "person";
}
dh.inherits(dh.model.Person, dh.model.GuidPersistable);

dh.model.Feed = function(id, displayName, photoUrl, homeUrl) {
	this.id = id;
	this.displayName = displayName;
	this.photoUrl = photoUrl;
	this.homeUrl = homeUrl;
	this.kind = "feed";
}
dh.inherits(dh.model.Feed, dh.model.GuidPersistable);

dh.model.Group = function(id, displayName, memberCount, photoUrl, homeUrl, isPublic) {
	this.id = id;
	this.displayName = displayName;
	this.memberCount = memberCount;
	this.photoUrl = photoUrl;
	this.homeUrl = homeUrl;
	this.isPublic = isPublic;
	this.kind = "group";
}
dh.inherits(dh.model.Group, dh.model.GuidPersistable);

dh.model.TheWorld = function() {
	this.id = "gaia"; /* Sufficiently odd that if this synthetic entity causes
	                     problems we should be able to see why immediately */
	this.displayName = "The World";
	this.kind = "world";
}
dh.inherits(dh.model.TheWorld, dh.model.GuidPersistable);

dh.model.splitCommaString = function(str) {
	return str.split(",");
}

dh.model.personFromXmlNode = function(element) {
	// the old HttpMethodsBean stuff returns a person that can be a contact or user,
	// the new method on PersonView returns either a <user> or <resource> element
	if (element.nodeName != "person" && element.nodeName != "user")
		dh.raise("not a person or user element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("display");
	if (!displayName)
		displayName = element.getAttribute("name"); // for <user>
	var photoUrl = element.getAttribute("photoUrl");
	if (!photoUrl)
		photoUrl = element.getAttribute("smallPhotoUrl"); // for <user>
	var hasAccount = element.getAttribute("hasAccount");
	if (element.nodeName == "user")
		hasAccount = "true";
	var userId = element.getAttribute("userId")
	if (!userId && element.nodeName == "user")
		userId = id;

	var homeUrl = element.getAttribute("homeUrl");
	if (!homeUrl)
		homeUrl = null;

	// the rest of this is all null for <user>		
	var contactId = element.getAttribute("contactId")
	var email = element.getAttribute("email");
	var aim = element.getAttribute("aim");
	var emails = element.getAttribute("emails");
	var aims = element.getAttribute("aims");

	if (emails)
		emails = dh.model.splitCommaString(emails);
	if (aims)
		aims = dh.model.splitCommaString(aims);

	// note, empty string is "false"	
	if (!id)
		dh.raise("no id attr on <person> node");
	if (!displayName)
		dh.raise("no display attr on <person> node");
	
	return new dh.model.Person(id, userId, displayName, email, aim, emails, aims, 
		hasAccount == "true" ? true : false, photoUrl, homeUrl);
}

dh.model.feedFromXmlNode = function(element) {
	if (element.nodeName != "feed")
		dh.raise("not a feed element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("name");
	var photoUrl = element.getAttribute("smallPhotoUrl");
	var homeUrl = element.getAttribute("homeUrl");

	// note, empty string is "false"	
	if (!id)
		dh.raise("no id attr on <feed> node");
	if (!displayName)
		dh.raise("no name attr on <person> node");
	
	return new dh.model.Feed(id, displayName, photoUrl, homeUrl);
}

dh.model.groupFromXmlNode = function(element) {
	if (element.nodeName != "group")
		dh.raise("not a group element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("display");
	var memberCount = element.getAttribute("memberCount");
	var photoUrl = element.getAttribute("photoUrl");
	var homeUrl = element.getAttribute("homeUrl");
	var isPublic = element.getAttribute("isPublic");

	// note, empty string is "false", so group name, which is what is usually
	// used for the display attribute, must not be blank	
	if (!id)
		dh.raise("no id attr on <group> node");
	if (!displayName)
		dh.raise("no display attr on <group> node");
	
	return new dh.model.Group(id, displayName, memberCount, photoUrl, homeUrl, isPublic);
}

dh.model.objectFromXmlNode = function(element) {
	if (element.nodeName == "person") {
		return dh.model.personFromXmlNode(element);
	} else if (element.nodeName == "group") {
		return dh.model.groupFromXmlNode(element);
	} else if (element.nodeName == "feed") { 
		return dh.model.feedFromXmlNode(element);
	} else if (element.nodeName == "user") {
		return dh.model.personFromXmlNode(element);
	} else {
		dh.raise("unknown xml node " + element.nodeName);
	}
}

dh.model.findGuid = function(set, id) {
	// set can be an array or a hash
	for (var prop in set) {
		if (dh.lang.has(set[prop], "id")) {
			if (id == set[prop]["id"]) {
				return prop;
			}
		}
	}
	return null;
}
