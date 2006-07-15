// This file contains the "model" objects, like Person
dojo.provide("dh.model");

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
	if (element.nodeName != "person")
		dojo.raise("not a person element");

	var id = element.getAttribute("id");
	var contactId = element.getAttribute("contactId")
	var userId = element.getAttribute("userId")
	var displayName = element.getAttribute("display");
	var email = element.getAttribute("email");
	var aim = element.getAttribute("aim");
	var emails = element.getAttribute("emails");
	var aims = element.getAttribute("aims");
	var photoUrl = element.getAttribute("photoUrl");
	var hasAccount = element.getAttribute("hasAccount");

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
