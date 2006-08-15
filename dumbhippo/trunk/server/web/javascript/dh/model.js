// This file contains the "model" objects, like Person
dojo.provide("dh.model");

dh.model.Track = function(image, title, artist, album, stillPlaying) {
	this.image = image;
	this.title = title;
	this.artist = artist;
	this.album = album;
	this.stillPlaying = stillPlaying;
}

dh.model.trackFromXmlNode = function(element) {
	if (element.nodeName != "song")
		dojo.raise("not a song element");
	var image = null;
	var title = null;
	var artist = null;
	var album = null;
	var stillPlaying = null;
	
	var i;
	for (i = 0; i < element.childNodes.length; ++i) {
		var n = element.childNodes.item(i);
		if (n.nodeName == "title")
			title = dojo.dom.textContent(n);
		else if (n.nodeName == "image")
			image = dojo.dom.textContent(n);
		else if (n.nodeName == "artist")
			artist = dojo.dom.textContent(n);
		else if (n.nodeName == "album")
			album = dojo.dom.textContent(n);
		else if (n.nodeName == "stillPlaying");
			stillPlaying = dojo.dom.textContent(n);
	}
	
	return new dh.model.Track(image, title, artist, album, stillPlaying == "true");
}

dh.model.Message = function(text, fromId, serial, timestamp) {
	this.text = text;
	this.fromId = fromId;
	this.serial = serial;
	this.timestamp = timestamp;
}

dh.model.messageFromXmlNode = function(element) {
	if (element.nodeName != "message")
		dojo.raise("not a message element");
	var text = dojo.dom.textContent(element);
	var from = element.getAttribute("fromId");
	var serial = element.getAttribute("serial");	
	var timestamp = element.getAttribute("timestamp");

	return new dh.model.Message(text, from, parseInt(serial), parseInt(timestamp));
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
dojo.inherits(dh.model.GuidPersistable, Object);

dh.model.Person = function(id, userId, displayName, email, aim, emails, aims, hasAccount, photoUrl) {
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
	this.kind = "person";
}
dojo.inherits(dh.model.Person, dh.model.GuidPersistable);

dh.model.Feed = function(id, displayName, photoUrl) {
	this.id = id;
	this.displayName = displayName;
	this.photoUrl = photoUrl;
	this.kind = "feed";
}
dojo.inherits(dh.model.Feed, dh.model.GuidPersistable);

dh.model.Group = function(id, displayName, sampleMembers, photoUrl) {
	this.id = id;
	this.displayName = displayName;
	this.sampleMembers = sampleMembers; // can be null
	this.photoUrl = photoUrl;
	this.kind = "group";
}
dojo.inherits(dh.model.Group, dh.model.GuidPersistable);

dh.model.TheWorld = function() {
	this.id = "gaia"; /* Sufficiently odd that if this synthetic entity causes
	                     problems we should be able to see why immediately */
	this.displayName = "The World";
	this.kind = "world";
}
dojo.inherits(dh.model.TheWorld, dh.model.GuidPersistable);

dh.model.splitCommaString = function(str) {
	return str.split(",");
}

dh.model.personFromXmlNode = function(element) {
	// the old HttpMethodsBean stuff returns a person that can be a contact or user,
	// the new method on PersonView returns either a <user> or <resource> element
	if (element.nodeName != "person" && element.nodeName != "user")
		dojo.raise("not a person or user element");

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
		dojo.raise("no id attr on <person> node");
	if (!displayName)
		dojo.raise("no display attr on <person> node");
	
	return new dh.model.Person(id, userId, displayName, email, aim, emails, aims, 
		hasAccount == "true" ? true : false, photoUrl);
}

dh.model.feedFromXmlNode = function(element) {
	if (element.nodeName != "feed")
		dojo.raise("not a feed element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("name");
	var photoUrl = element.getAttribute("smallPhotoUrl");

	// note, empty string is "false"	
	if (!id)
		dojo.raise("no id attr on <feed> node");
	if (!displayName)
		dojo.raise("no name attr on <person> node");
	
	return new dh.model.Feed(id, displayName, photoUrl);
}

dh.model.groupFromXmlNode = function(element) {
	if (element.nodeName != "group")
		dojo.raise("not a group element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("display");
	var sampleMembers = element.getAttribute("sampleMembers");
	var photoUrl = element.getAttribute("photoUrl");

	// note, empty string is "false", so group name, which is what is usually
	// used for the display attribute, must not be blank	
	if (!id)
		dojo.raise("no id attr on <group> node");
	if (!displayName)
		dojo.raise("no display attr on <group> node");
	
	return new dh.model.Group(id, displayName, sampleMembers, photoUrl);
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
		dojo.raise("unknown xml node " + element.nodeName);
	}
}

dh.model.findGuid = function(set, id) {
	// set can be an array or a hash
	for (var prop in set) {
		if (dojo.lang.has(set[prop], "id")) {
			if (id == set[prop]["id"]) {
				return prop;
			}
		}
	}
	return null;
}
