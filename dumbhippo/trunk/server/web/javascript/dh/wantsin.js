dojo.provide("dh.wantsin")
dojo.require("dh.server")
dojo.require("dh.formtable")
dojo.require("dh.textinput")
dojo.require("dojo.dom")


dh.wantsin.send = function() {
	var address = dojo.string.trim(dh.wantsin.dhWantsInEmailEntry.getValue())

	if (address == "" || address.indexOf("@") < 0) {
		alert("Please enter a valid email address")
		return false;
	}
	return true;
}

dhWantsInInit = function() {
	dh.wantsin.dhWantsInEmailEntry = new dh.textinput.Entry(document.getElementById("dhWantsInEmailEntry"), "email@example.com")
}

dojo.event.connect(dojo, "loaded", dj_global, "dhWantsInInit");
