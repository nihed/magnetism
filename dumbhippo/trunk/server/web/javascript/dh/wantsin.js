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

	/*
	dh.server.getXmlPOST("wantsin",
	        {
        		"address" : address
        	},
        	function(type, document, http) {
        		var messages = document.getElementsByTagName("message")
        	        if (messages.length > 0) {
				dh.formtable.showStatusMessage("dhWantsInEmailEntry",messages[0],false);
        	        } else {
               			dojo.debug("Didn't get message in response to wantsin");
                	}
        	},
        	function(type, error, http) {
        		dojo.debug("sendemailinvitation got back error " + dhAllPropsAsString(error));
        	}
	)
	*/

}


dhWantsInInit = function() {
	dh.wantsin.dhWantsInEmailEntry = new dh.textinput.Entry(document.getElementById("dhWantsInEmailEntry"), "email@example.com")


}