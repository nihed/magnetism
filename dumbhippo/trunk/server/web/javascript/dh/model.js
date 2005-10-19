// This file contains the "model" objects, like Person
dojo.provide("dh.model");

dh.model.Person = function(id, displayName) {
	this.id = id;
	this.displayName = displayName;
}
dojo.inherits(dh.model.Person, Object);

dh.model.personFromXmlNode = function(element) {
	var id = element.getAttribute("id");
	var displayName = element.getAttribute("display");
	
	if (!id)
		dojo.raise("no id attr on <person> node");
	if (!displayName)
		dojo.raise("no display attr on <person> node");
	
	return new dh.model.Person(id, displayName);
}
