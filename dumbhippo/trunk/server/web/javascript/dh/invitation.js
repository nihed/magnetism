dojo.provide("dh.invitation")
dojo.require("dh.server")
dojo.require("dh.textinput")
dojo.require("dojo.string")
dojo.require("dojo.dom");
dojo.require("dh.util")

dh.invitation.suggestGroupsPopup = null;
dh.invitation.suggestGroupsArea = null;
dh.invitation.suggestGroupsInvitee = null;
dh.invitation.suggestGroupsOk = null;
dh.invitation.suggestGroupsCancel = null;

dh.invitation.suggestGroupsLinkId = null;
dh.invitation.suggestGroupsLinkHref = null;
dh.invitation.suggestGroupsLinkValue = null;
dh.invitation.suggestedGroupIdsArray = null;

dh.invitation.suggestedGroupIdsWithInvitationArray = null;
dh.invitation.suggestedGroupsWithInvitation = null;

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
	
	var commaSuggestedGroups = dh.util.join(dh.invitation.suggestedGroupIdsWithInvitationArray, ",")
	
    dh.server.getXmlPOST("sendemailinvitation",
                    {
                        "address" : address,
                        "subject" : subject,
	                    "message" : message,
	                    "suggestedGroupIds" : commaSuggestedGroups
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
                    	alert("Couldn't send the email invitation");
  	                })
}

dh.invitation.fillValues = function(values) {
	dh.invitation.subjectEntry.setValue(values["dhSubjectEntry"])
	dh.invitation.messageEntry.setValue(values["dhMessageEntry"])
}

dh.invitation.resend = function(address) {
	dh.invitation.fillValues(dh.invitation.resendValues)
	dh.invitation.addressEntry.setValue(address)
	// if the user is out of invitations, they can only resend an invitation,
	// so subject entry, message entry and send button are enabled only if
	// it is a resend; address entry stays disabled, pre-filled with the 
	// invitee e-mail 
    dh.invitation.subjectEntry.setEnabled()
    dh.invitation.messageEntry.setEnabled() 
    document.getElementById("dhInvitationSendButton").disabled = false
	dh.util.showMessage('Press "Send" to resend the invitation')
	
	// Scroll to the top of the page
	scroll(0,0)
}

dh.invitation.showSuggestGroupsPopup = function(linkId, address, suggestedGroupIds) {
    
    // replace suggest groups link for the previous invitee, if we are showing 
    // the group suggestions popup for a new invitee  
    if (dh.invitation.suggestGroupsLinkId) {
        var prevSuggestGroupsLink = document.getElementById(dh.invitation.suggestGroupsLinkId)
        prevSuggestGroupsLink.href = dh.invitation.suggestGroupsLinkHref
        dojo.dom.textContent(prevSuggestGroupsLink, dh.invitation.suggestGroupsLinkValue);
    }
    
    var suggestGroupsLink = document.getElementById(linkId)
        
    dh.invitation.suggestGroupsLinkId = linkId;
    dh.invitation.suggestGroupsLinkHref = suggestGroupsLink.href
    dh.invitation.suggestGroupsLinkValue = suggestGroupsLink.firstChild.nodeValue
    
    suggestGroupsLink.href = "javascript:dh.invitation.cancelSuggestGroups()"
    
    dojo.dom.textContent(suggestGroupsLink, "Cancel");
    
    dh.util.show(dh.invitation.suggestGroupsPopup)
    var inviteeAddressNode = document.createTextNode(address)
    if (dh.invitation.suggestGroupsInvitee.firstChild == null) {
        dh.invitation.suggestGroupsInvitee.appendChild(inviteeAddressNode)
    } else {     
        dh.invitation.suggestGroupsInvitee.replaceChild(inviteeAddressNode, dh.invitation.suggestGroupsInvitee.firstChild)
    }
    
    if (linkId == "dhSuggestGroupsWithInvitation") {
        dh.invitation.suggestedGroupIdsArray = dh.invitation.suggestedGroupIdsWithInvitationArray
    } else {
        dh.invitation.suggestedGroupIdsArray = suggestedGroupIds.split(",");
    }
    
    var allInputs = document.getElementsByTagName("input");
    
    for (var i = 0; i < allInputs.length; ++i) {
        var n = allInputs[i];
        if (n.type == "checkbox" && dh.util.contains(dh.invitation.suggestedGroupIdsArray, n.value)) {
            n.checked = true
        } else if (n.type == "checkbox") {
            n.checked = false
        }
    }  
    
    // display the border around the group listing only if there is a scrollbar
    if (dh.invitation.suggestGroupsArea.scrollHeight < dh.invitation.suggestGroupsArea.offsetHeight) {
        dh.invitation.suggestGroupsArea.style.border = "0px"
    }
    
    // scroll to the top of the suggest groups area
    dh.invitation.suggestGroupsArea.scrollTop = 0
}

dh.invitation.doSuggestGroups = function() {
    var allInputs = document.getElementsByTagName("input")
    var suggestedGroups = []
    var desuggestedGroups = []
    var suggestedGroupsWithInvitationArray = []
    for (var i = 0; i < allInputs.length; ++i) {
        var n = allInputs[i]
        if (n.type == "checkbox" && n.checked) {
            if (dh.invitation.suggestGroupsLinkId == "dhSuggestGroupsWithInvitation") {
                suggestedGroups.push(n.value)                            
                suggestedGroupsWithInvitationArray.push(" " + dojo.string.trim(n.nextSibling.nodeValue))
            } else if (!dh.util.contains(dh.invitation.suggestedGroupIdsArray, n.value)) {
                // if this is a new suggestion, add it to the list;
                // remove the last condition if we want to "renew" all suggestions, it does not seem necessary,
                // since this is happenning at the point when the person does not have an account yet, there 
                // is no difference between a new suggestion and a renewed suggestion
                suggestedGroups.push(n.value)
            }
        } else if (n.type == "checkbox" && !n.checked && dh.util.contains(dh.invitation.suggestedGroupIdsArray, n.value)) {
            desuggestedGroups.push(n.value)
        }
    }    
      
    if (dh.invitation.suggestGroupsLinkId == "dhSuggestGroupsWithInvitation") {
        dh.invitation.suggestedGroupIdsWithInvitationArray = suggestedGroups
       
        var commaSuggestedGroupsWithInvitationNode = document.createTextNode(dojo.string.trim(dh.util.join(suggestedGroupsWithInvitationArray, ",")) + " | ")
        if (dh.invitation.suggestedGroupsWithInvitation.firstChild == null) {
            dh.invitation.suggestedGroupsWithInvitation.appendChild(commaSuggestedGroupsWithInvitationNode)
        } else {     
            dh.invitation.suggestedGroupsWithInvitation.replaceChild(commaSuggestedGroupsWithInvitationNode, dh.invitation.suggestedGroupsWithInvitation.firstChild)
        }
        
        dh.util.hide(dh.invitation.suggestGroupsPopup)
        
        var suggestGroupsLink = document.getElementById("dhSuggestGroupsWithInvitation")
        suggestGroupsLink.href = dh.invitation.suggestGroupsLinkHref
        if (suggestedGroups.length > 0) {
            dojo.dom.textContent(suggestGroupsLink, "Edit")
        } else {
            dojo.dom.textContent(suggestGroupsLink, "Choose Groups")        
        }
       
        // set these values back to null, so that we will not reuse them when the group suggestions
        // popup is shown next time
        dh.invitation.suggestGroupsLinkId = null
        dh.invitation.suggestGroupsLinkHref = null
        dh.invitation.suggestGroupsLinkValue = null
        dh.invitation.suggestedGroupIdsArray = null    
    } else {
    
	    var commaSuggestedGroups = dh.util.join(suggestedGroups, ",")
	    var commaDesuggestedGroups = dh.util.join(desuggestedGroups, ",")
	        
        dh.server.doPOST("suggestgroups",
		   		         { "address" : dh.invitation.suggestGroupsInvitee.firstChild.nodeValue, 
					       "suggestedGroupIds" : commaSuggestedGroups,
					       "desuggestedGroupIds" : commaDesuggestedGroups },
		  	    	     function(type, data, http) {
		  	    	         // we do not want the information to be resent with a reload, 
		  	    	         // we want to get a new view of the page
		  	    	         document.location.href = "/invitation";
		  	    	     },
		  	    	     function(type, error, http) {
		  	    	         alert("Couldn't add group suggestions");
		  	    	         dh.invitation.cancelSuggestGroups();
		  	    	     })
    }
}

dh.invitation.cancelSuggestGroups = function() {
    dh.util.hide(dh.invitation.suggestGroupsPopup)
    
    var suggestGroupsLink = document.getElementById(dh.invitation.suggestGroupsLinkId)
    suggestGroupsLink.href = dh.invitation.suggestGroupsLinkHref
    dojo.dom.textContent(suggestGroupsLink, dh.invitation.suggestGroupsLinkValue)
    
    // set these values back to null, so that we will not reuse them when the group suggestions
    // popup is shown next time
    dh.invitation.suggestGroupsLinkId = null
    dh.invitation.suggestGroupsLinkHref = null
    dh.invitation.suggestGroupsLinkValue = null
    dh.invitation.suggestedGroupIdsArray = null
}

dhInvitationInit = function() {
	dh.invitation.addressEntry = new dh.textinput.Entry(document.getElementById("dhAddressEntry"), "myfriend@example.com")
	dh.invitation.subjectEntry = new dh.textinput.Entry(document.getElementById("dhSubjectEntry"))
	dh.invitation.messageEntry = new dh.textinput.Entry(document.getElementById("dhMessageEntry"))
	
	dh.invitation.fillValues(dh.invitation.initialValues)
	
	dh.invitation.suggestGroupsPopup = document.getElementById("dhSuggestGroupsPopup")
	dh.invitation.suggestGroupsArea = document.getElementById("dhSuggestGroupsArea")
	dh.invitation.suggestGroupsInvitee = document.getElementById("dhSuggestGroupsInvitee")	
	dh.invitation.suggestGroupsOk = document.getElementById("dhSuggestGroupsOk")
	dh.invitation.suggestGroupsCancel = document.getElementById("dhSuggestGroupsCancel")
	dh.invitation.suggestedGroupsWithInvitation = document.getElementById("dhSuggestedGroupsWithInvitation")
	
	dh.invitation.suggestedGroupIdsWithInvitationArray = []
}
