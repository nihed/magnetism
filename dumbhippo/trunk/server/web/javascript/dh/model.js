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
}
dojo.inherits(dh.model.GuidPersistable, Object);

dh.model.Person = function(id, displayName) {
	dh.model.GuidPersistable.call(id, displayName);
	this.kind = "person";
}
dojo.inherits(dh.model.Person, dh.model.GuidPersistable);

dh.model.Group = function(id, displayName) {
	dh.model.GuidPersistable.call(id, displayName);
	this.kind = "group";
}
dojo.inherits(dh.model.Group, dh.model.GuidPersistable);

dh.model.personFromXmlNode = function(element) {
	if (element.nodeName != "person")
		dojo.raise("not a person element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("display");
	
	if (!id)
		dojo.raise("no id attr on <person> node");
	if (!displayName)
		dojo.raise("no display attr on <person> node");
	
	return new dh.model.Person(id, displayName);
}

dh.model.groupFromXmlNode = function(element) {
	if (element.nodeName != "group")
		dojo.raise("not a group element");

	var id = element.getAttribute("id");
	var displayName = element.getAttribute("display");
	
	if (!id)
		dojo.raise("no id attr on <group> node");
	if (!displayName)
		dojo.raise("no display attr on <group> node");
	
	return new dh.model.Group(id, displayName);
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
