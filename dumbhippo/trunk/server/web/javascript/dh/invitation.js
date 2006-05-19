dojo.provide("dh.invitation")
dojo.require("dh.server")
dojo.require("dh.textinput")
dojo.require("dojo.string")

dh.invitation.showMessage = function(message) {
	var div = document.getElementById("dhMessageDiv")
	
	dh.util.clearNode(div)
	div.appendChild(document.createTextNode(message))
	div.style.display = "block"
}

dh.invitation.reloadWithMessage = function(message) {
	// We do this as a POST to avoid including the message in the URL
	document.getElementById("dhReloadMessage").value = message
	document.forms["dhReloadForm"].submit()
}

dh.invitation.showInfo = function(message) {
	var div = document.getElementById("dhInfoDiv")
	
	dh.util.clearNode(div)
	div.appendChild(document.createTextNode(message))
	div.style.display = "block"
}

dh.invitation.send = function() {
	var address = dojo.string.trim(dh.invitation.addressEntry.getValue())
	var subject = dojo.string.trim(dh.invitation.subjectEntry.getValue())
	var message = dojo.string.trim(dh.invitation.messageEntry.getValue())
	
	if (address == "" || address.indexOf("@") < 0) {
		alert("Please enter a valid email address")
		return
	}
	
    dh.server.getXmlPOST("sendemailinvitation",
                    {
                        "address" : address,
                        "subject" : subject,
	                    "message" : message
                    },
                    function(type, document, http) {
                    	var messages = document.getElementsByTagName("message")
                    	if (messages.length > 0) {
                    		dh.invitation.reloadWithMessage(dojo.dom.textContent(messages[0]))
                		} else {
	                        dojo.debug("Didn't get message in response to sendemailinvitation");
                		}
                    },
                    function(type, error, http) {
                        dojo.debug("sendemailinvitation got back error " + dhAllPropsAsString(error));
  	                })
}

dh.invitation.fillValues = function(values) {
	dh.invitation.subjectEntry.setValue(values["dhSubjectEntry"])
	dh.invitation.messageEntry.setValue(values["dhMessageEntry"])
}

dh.invitation.resend = function(address) {
	dh.invitation.fillValues(dh.invitation.resendValues)
	dh.invitation.addressEntry.setValue(address)
	dh.invitation.showMessage('Press "Send" to resend the invitation')
	
	// Scroll to the top of the page
	scroll(0,0)
}

dhInvitationInit = function() {
	dh.invitation.addressEntry = new dh.textinput.Entry(document.getElementById("dhAddressEntry"), "myfriend@example.com")
	dh.invitation.subjectEntry = new dh.textinput.Entry(document.getElementById("dhSubjectEntry"))
	dh.invitation.messageEntry = new dh.textinput.Entry(document.getElementById("dhMessageEntry"))
	
	dh.invitation.fillValues(dh.invitation.initialValues)
}
