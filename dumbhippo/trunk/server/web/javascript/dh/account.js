dojo.provide("dh.account");
dojo.require("dh.formtable");
dojo.require("dh.textinput");
dojo.require("dh.fileinput");
dojo.require("dh.photochooser");
dojo.require("dh.lovehate");
dojo.require("dh.love");
dojo.require("dh.password");
dojo.require("dh.util");
dojo.require("dh.dom");
dojo.require("dh.event");

dh.account.generatingRandomBio = false;
dh.account.generateRandomBio = function() {
	if (dh.account.generatingRandomBio) {
		dh.formtable.showStatusMessage('dhBioEntry', "Working on it - be patient!");
		return;
	}

	dh.formtable.showStatus('dhBioEntry', "Generating random bio...", {}, {}, {});
	dh.account.generatingRandomBio = true;
	dh.server.getTextGET("randombio", 
						{ },
						function(type, data, http) {
							dh.formtable.showStatusMessage('dhBioEntry', "Tada! Random bio!");
							dh.account.generatingRandomBio = false;
							// focus and set the new text
							dh.account.bioEntryNode.select();
							// don't emit changed until user causes it
							dh.account.bioEntry.setValue(data, true);
						},
						function(type, error, http) {
							dh.formtable.showStatusMessage('dhBioEntry', "Failed to generate random bio - we suck, sorry! Try again soon.");
		  	    	 		dh.account.generatingRandomBio = false;
						});
}


dh.account.verifyEmail = function() {
	var emailEntryNode = document.getElementById('dhEmailEntry');
	if (emailEntryNode.value.indexOf("@") < 0) {
		dh.formtable.showStatusMessage('dhEmailEntry', "Enter an email address, then click Verify");
		return;
	}
	var address = emailEntryNode.value;
  	dh.server.doPOST("sendclaimlinkemail",
			 	     { "address" : address },
  					 function(type, data, http) {
	  					 dh.formtable.showStatusMessage('dhEmailEntry', "We sent mail to '" + address + "', click on the link in that mail.");
	  					 emailEntryNode.value = "";
					 },
					 function(type, error, http) {
						 dh.formtable.showStatusMessage('dhEmailEntry', "Failed to send mail - check the address, or just try again...");
					 });
}

dh.account.removeClaimEmail = function(address) {
  	dh.server.doPOST("removeclaimemail",
			 	     { "address" : address },
  					 function(type, data, http) {
  					 	dh.util.refresh();
					 },
					 function(type, error, http) {
						 alert("Couldn't remove this address.");
					 });
}

dh.account.removeClaimAim = function(address) {
  	dh.server.doPOST("removeclaimaim",
			 	     { "address" : address },
  					 function(type, data, http) {
  					 	dh.util.refresh();
					 },
					 function(type, error, http) {
						 alert("Couldn't remove this address.");
					 });
}

dh.account.verifyXmpp = function() {
	// Form got submitted when the XMPP entry wasn't active
	if (dh.account.imAccountType == 'aim')
		return;

	var address = dh.account.imEntry.getValue();
	if (address.indexOf("@") < 0) {
		alert("Enter an address, then click Verify");
		return;
	}
  	dh.server.doPOST("sendclaimlinkxmpp",
			 	     { "address" : address },
  					 function(type, data, http) {
  					 	 dh.account.imEntry.setValue(""); 
						 dh.account.closeImAccountPopup();
						 
						 var imTableBody = document.getElementById("dhImTableBody");
						 var row = document.createElement("tr");
						 imTableBody.appendChild(row);
						 var cell = document.createElement("td");
 						 cell.className = "dh-im-pending-address";
						 row.appendChild(cell);
						 cell.appendChild(document.createTextNode(address));
						 cell = document.createElement("td");
						 row.appendChild(cell);
						 var anchor = document.createElement("a");
						 cell.appendChild(anchor);
						 anchor.appendChild(document.createTextNode("cancel"));
						 anchor.href = "javascript:void";
						 anchor.onclick = function() { dh.account.removeClaimXmpp(address) };
						 
						 row = document.createElement("tr");
						 imTableBody.appendChild(row);
						 cell = document.createElement("td");
						 row.appendChild(cell);
						 cell.appendChild(document.createTextNode("You've been sent a verify link"));
						 cell.className = "dh-im-verify-message";
						 
						 row = document.createElement("tr");
						 row.className = "dh-email-address-spacer";
						 imTableBody.appendChild(row);
					 },
					 function(type, error, http) {
					 	 alert("Internal error talking to '" + address + "'");
					 });
}

dh.account.removeClaimXmpp = function(address) {
  	dh.server.doPOST("removeclaimxmpp",
			 	     { "address" : address },
  					 function(type, data, http) {
  					 	dh.util.refresh();
					 },
					 function(type, error, http) {
						 alert("Couldn't remove this address.");
					 });
}

dh.account.createImEntry = function() {
    if (dh.util.exists('dhXmppEntry')) {
        dh.account.imEntry = new dh.textinput.Entry(document.getElementById('dhXmppEntry'), 'your.name@example.com', '');
   	    dh.account.imAccountType = 'aim';
   	}
}

dh.account.showImAccountPopup = function() {
	var aboveNode = document.getElementById("dhAddImLink");
	dh.popup.show("dhAddImPopup", aboveNode);
    dh.account.imPopupRefresh = false;
}

dh.account.closeImAccountPopup = function() {
	dh.popup.hide("dhAddImPopup");
	if (dh.account.imPopupRefresh)
		dh.util.refresh();
}

dh.account.setImAccountType = function(type) {
	dh.account.imAccountType = type;

	var aimContent = document.getElementById("dhAddAimContent");
	aimContent.style.display = (type == "aim") ? "block" : "none";

	var xmppContent = document.getElementById("dhAddXmppContent");
	xmppContent.style.display = (type != "aim") ? "block" : "none";
	
	if (type == 'gtalk') {
		dh.account.imEntry.setDefaultText("your.name@gmail.com");
	} else {
		dh.account.imEntry.setDefaultText("your.name@example.com");
	}
}

dh.account.hateExternalAccount = function(type, quip, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("hateexternalaccount",
				     { "type" : type,
				       "quip" :  quip },
					loadFunc, errorFunc);
}

dh.account.removeExternalAccount = function(type, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("removeexternalaccount",
				     { "type" : type },
						loadFunc, errorFunc);
}
dh.account.findFlickrAccount = function(email, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("findflickraccount",
				     { "email" : email },
						loadFunc, errorFunc);
}

dh.account.setFlickrAccount = function(nsid, email, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setflickraccount",
				     { "nsid" : nsid, "email" : email },
				     	loadFunc, errorFunc);
}
dh.account.setLinkedInProfile = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setlinkedinaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setMyspaceName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setmyspaceaccount",
				     { "name" : name },
						loadFunc, errorFunc);
}
dh.account.setYouTubeName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setyoutubeaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setLastFmName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setlastfmaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setDeliciousName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setdeliciousaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setTwitterName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("settwitteraccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setDiggName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setdiggaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setRedditName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setredditaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setRhapsodyUrl = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setrhapsodyaccount",
   	                      { "url" : name },
   	                      loadFunc, errorFunc);
}
dh.account.setNetflixUrl = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setnetflixaccount",
   	                      { "url" : name },
   	                      loadFunc, errorFunc);
}
 
dh.account.setGoogleReaderUrl = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setgooglereaderaccount",
   	                      { "url" : name },
   	                      loadFunc, errorFunc);
}
dh.account.setPicasaName = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setpicasaaccount",
				     { "urlOrName" : name },
						loadFunc, errorFunc);
}

dh.account.setAmazonUrl = function(name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setamazonaccount",
				          { "urlOrUserId" : name },
						  loadFunc, errorFunc);
}

dh.account.createExternalAccountOnHateSavedFunc = function(entry, accountType) {
	return function(value) {
		var oldMode = entry.getMode();
		entry.setBusy();
		dh.account.hateExternalAccount(accountType, value,
		 	    	 function(childNodes, http) {
		 	    	 	entry.setMode('hate');
		 	    	 	if (accountType == 'AMAZON') {
	 	    	            var amazonDetailsNodes = dh.html.getElementsByClass('dh-amazon-details');
	                        var i = 0;
	                        for (i = 0; i < amazonDetailsNodes.length; ++i) {
	                            var amazonDetailsNode = amazonDetailsNodes[i];
	 	    	                dh.dom.removeChildren(amazonDetailsNode);
	 	    	            }    
	 	    	        }
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	alert(msg);
		  	    	 	entry.setMode(oldMode);
		  	    	 });
	}
}

dh.account.createExternalAccountOnCanceledFunc = function(entry, accountType) {
	return function(value) {
		var oldMode = entry.getMode();
		entry.setBusy();
		dh.account.removeExternalAccount(accountType, 
		 	    	 function(childNodes, http) {
		 	    	 	entry.setMode('indifferent');
		 	    	 	if (accountType == 'AMAZON') {
	 	    	            var amazonDetailsNodes = dh.html.getElementsByClass('dh-amazon-details');
	                        var i = 0;
	                        for (i = 0; i < amazonDetailsNodes.length; ++i) {
	                            var amazonDetailsNode = amazonDetailsNodes[i];
	 	    	                dh.dom.removeChildren(amazonDetailsNode);
	 	    	            }    
	 	    	        }
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	alert(msg);
		  	    	 	entry.setMode(oldMode);
		  	    	 });
	}
}

dh.account.onFlickrLoveSaved = function(value) {
	var entry = dh.account.flickrEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
	dh.account.findFlickrAccount(value,
			function(childNodes, http) {

				// change child nodes to be children of flickrUser
			    childNodes = childNodes.item(0).childNodes;
			    var nsid = null;
			    var username = null;
				var i = 0;
				for (i = 0; i < childNodes.length; ++i) {
					var child = childNodes.item(i);
					if (child.nodeType != dh.dom.ELEMENT_NODE)
						continue;
		
					if (child.nodeName == "nsid") {
						nsid = dh.dom.textContent(child);
					} else if (child.nodeName == "username") {
						username = dh.dom.textContent(child);
					}
				}
				
				dh.account.setFlickrAccount(nsid, value,
					function(childNodes, http) {
						entry.setMode('love');
					},
					function(code, msg, http) {
						alert(msg);
			  	     	entry.setMode(oldMode);			
					});
			},
	  	    function(code, msg, http) {
	  	        alert(msg);
	  	     	entry.setMode(oldMode);
	  	    });
}

dh.account.onMyspaceLoveSaved = function(value) {
	var entry = dh.account.myspaceEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setMyspaceName(value, 
	 	    	 function(childNodes, http) {
	 	    	     var i = 0;
					 for (i = 0; i < childNodes.length; ++i) {
						 var child = childNodes.item(i);
						 if (child.nodeType != dh.dom.ELEMENT_NODE)
							 continue;
			
						 if (child.nodeName == "message") {
							 msg = dh.dom.textContent(child);
							 alert(msg);
							 break;
						 }
	 	    	 	 }
	 	    	 	 entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	 alert(msg);
	  	    	 	 entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onYouTubeLoveSaved = function(value) {
	var entry = dh.account.youTubeEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setYouTubeName(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLastFmLoveSaved = function(value) {
	var entry = dh.account.lastFmEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setLastFmName(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setError(null);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	entry.setError(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLinkedInLoveSaved = function(value) {
	var entry = dh.account.linkedInEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setLinkedInProfile(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dh.dom.textContent(child);
						}
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onRhapsodyLoveSaved = function(value) {
	var entry = dh.account.rhapsodyEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setRhapsodyUrl(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onDeliciousLoveSaved = function(value) {
	var entry = dh.account.deliciousEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setDeliciousName(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dh.dom.textContent(child);
						}
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onTwitterLoveSaved = function(value) {
	var entry = dh.account.twitterEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setTwitterName(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dh.dom.textContent(child);
						}
						
						if (child.nodeName == "message") {
							msg = dh.dom.textContent(child);
							alert(msg);
						}
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onDiggLoveSaved = function(value) {
	var entry = dh.account.diggEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setDiggName(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dh.dom.textContent(child);
						}
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onRedditLoveSaved = function(value) {
	var entry = dh.account.redditEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setRedditName(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dh.dom.textContent(child);
						}

						 if (child.nodeName == "message") {
							 msg = dh.dom.textContent(child);
							 alert(msg);
						 }
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onNetflixLoveSaved = function(value) {
	var entry = dh.account.netflixEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setNetflixUrl(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onGoogleReaderLoveSaved = function(value) {
	var entry = dh.account.googleReaderEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setGoogleReaderUrl(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onPicasaLoveSaved = function(value) {
	var entry = dh.account.picasaEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setPicasaName(value,
	 	    	 function(childNodes, http) {
				    var username = null;
					var i = 0;
					for (i = 0; i < childNodes.length; ++i) {
						var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
							continue;
			
						if (child.nodeName == "username") {
							username = dh.dom.textContent(child);
						}
	 	    	 	}
					entry.setLoveValueAlreadySaved(username);
	 	    	 	entry.setMode('love');
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onAmazonLoveSaved = function(value) {
	var entry = dh.account.amazonEntry;
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setAmazonUrl(value, 
	 	    	 function(childNodes, http) {
	 	    	 	entry.setMode('love');
	 	    	    var amazonDetailsNodes = dh.html.getElementsByClass('dh-amazon-details');
	                var i = 0;
	                for (i = 0; i < amazonDetailsNodes.length; ++i) {
	                    var amazonDetailsNode = amazonDetailsNodes[i];
	 	    	        dh.dom.removeChildren(amazonDetailsNode);
					    var j = 0;
					    for (j = 0; j < childNodes.length; ++j) {
						    var child = childNodes.item(j);
						    if (child.nodeType != dh.dom.ELEMENT_NODE)
							    continue;
			
						    if (child.nodeName == "amazonDetails") {
						        var k = 0;
						        for (k = 0; k < child.childNodes.length; ++k) {
						            var linkChild = child.childNodes.item(k);
						            if (linkChild.nodeType != dh.dom.ELEMENT_NODE)
							            continue;			
						
						            if (linkChild.nodeName == "link") {
						                var nameNode = linkChild.firstChild;
						                var urlNode = linkChild.lastChild;
						                if (nameNode.nodeName != "name" || urlNode.nodeName != "url")
						                    continue;

						                var linkDiv = document.createElement("div");
						                amazonDetailsNode.appendChild(linkDiv);
						                var linkElement = document.createElement("a");
                                        linkElement.href = dh.util.getPreparedUrl(dh.dom.textContent(urlNode));						            
                                        var linkTextNode = document.createTextNode(dh.dom.textContent(nameNode));
                                        linkElement.appendChild(linkTextNode);
                                        linkDiv.appendChild(linkElement);						                 
                                    }
                                }
						    }
	 	    	 	    }	 	    	    
	 	    	    }
	 	    	    
	 	    	    for (i = 0; i < childNodes.length; ++i) {
				        var child = childNodes.item(i);
						if (child.nodeType != dh.dom.ELEMENT_NODE)
					        continue;
			
						if (child.nodeName == "message") {
							msg = dh.dom.textContent(child);
							alert(msg);
							break;
					    }
	 	    	 	}	
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.aimVerify = function() {
	dh.server.getTextGET("aimVerifyLink", 
						{ },
						function(type, data, http) {
							window.open(data, "_self");
						},
						function(type, error, http) {
							alert("Couldn't get link to verify AIM account");
						});

	// Once you've IM'ed our bot, we want to refresh the page after the IM popup is closed						
    dh.account.imPopupRefresh = true;
						
}

dh.account.disableFacebookSession = function() {   
  	dh.server.doPOST("disablefacebooksession",
			 	     {},
  					 function(type, data, http) {
  					    // it is important that we "loose" the authentication token here,
  					    // otherwise we'll end up processing it again, and re-login the user
  					 	window.open("/account", "_self", null, true);
					 },
					 function(type, error, http) {
						 alert("Couldn't disabled the Facebook session.");
					 });    
}

dh.account.createMyspaceEntry = function() {
    dh.account.myspaceEntry = new dh.lovehate.Entry('dhMySpace', 'Myspace username', dh.account.initialMyspaceName,
							'I despise Tom and his space', dh.account.initialMyspaceHateQuip, 'Your friends get updates when you post to your MySpace blog.');
	dh.account.myspaceEntry.onLoveSaved = dh.account.onMyspaceLoveSaved;
	dh.account.myspaceEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.myspaceEntry, 'MYSPACE');
	dh.account.myspaceEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.myspaceEntry, 'MYSPACE');
}

dh.account.createYouTubeEntry = function() {
    dh.account.youTubeEntry = new dh.lovehate.Entry('dhYouTube', 'YouTube username or profile URL', dh.account.initialYouTubeName,
							'Video should kill the internet geeks', dh.account.initialYouTubeHateQuip, 'Your friends get updates when you upload new videos.');
	dh.account.youTubeEntry.onLoveSaved = dh.account.onYouTubeLoveSaved;
	dh.account.youTubeEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.youTubeEntry, 'YOUTUBE');
	dh.account.youTubeEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.youTubeEntry, 'YOUTUBE');
}

dh.account.createLastFmEntry = function() {	
	dh.account.lastFmEntry = new dh.lovehate.Entry('dhLastFm', 'Last.fm username', dh.account.initialLastFmName,
					'Uhh...what\'s Last.fm?', dh.account.initialLastFmHateQuip, 'Your friends see what music you\'re listening to.');
	dh.account.lastFmEntry.onLoveSaved = dh.account.onLastFmLoveSaved;
	dh.account.lastFmEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.lastFmEntry, 'LASTFM');
	dh.account.lastFmEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.lastFmEntry, 'LASTFM');		
}

dh.account.createFlickrEntry = function() {		
	dh.account.flickrEntry = new dh.lovehate.Entry('dhFlickr', 'Email used for Flickr account', dh.account.initialFlickrEmail,
					'Flickr doesn\'t do it for me', dh.account.initialFlickrHateQuip, 'Your friends get updates when you upload new photos and photo sets.');
	dh.account.flickrEntry.onLoveSaved = dh.account.onFlickrLoveSaved;
	dh.account.flickrEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.flickrEntry, 'FLICKR');
	dh.account.flickrEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.flickrEntry, 'FLICKR');
}

dh.account.createLinkedInEntry = function() {	
	dh.account.linkedInEntry = new dh.lovehate.Entry('dhLinkedIn', 'LinkedIn username or profile URL', dh.account.initialLinkedInName,
					'LinkedIn is for nerds', dh.account.initialLinkedInHateQuip);
	dh.account.linkedInEntry.onLoveSaved = dh.account.onLinkedInLoveSaved;
	dh.account.linkedInEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.linkedInEntry, 'LINKED_IN');
	dh.account.linkedInEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.linkedInEntry, 'LINKED_IN');	
}

dh.account.createRhapsodyEntry = function() {	
	dh.account.rhapsodyEntry = new dh.lovehate.Entry('dhRhapsody', 'Rhapsody \u201CRecently Played Tracks\u201D RSS feed URL', dh.account.initialRhapsodyUrl,
					'All-you-can-eat music services hurt my diet', dh.account.initialRhapsodyHateQuip, 
					'Your friends will see updates from your Rhapsody playlist.',
                    'http://www.rhapsody.com/myrhapsody/rss.html');
	dh.account.rhapsodyEntry.setSpecialLoveValue("My Recently Played Tracks");				
	dh.account.rhapsodyEntry.onLoveSaved = dh.account.onRhapsodyLoveSaved;
	dh.account.rhapsodyEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.rhapsodyEntry, 'RHAPSODY');
	dh.account.rhapsodyEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.rhapsodyEntry, 'RHAPSODY');	
}

dh.account.createDeliciousEntry = function() {	
	dh.account.deliciousEntry = new dh.lovehate.Entry('dhDelicious', 'del.icio.us username or profile URL', dh.account.initialDeliciousName,
					'del.icio.us isn\'t', dh.account.initialDeliciousHateQuip, 'Your friends get updates when you add public bookmarks.');
	dh.account.deliciousEntry.onLoveSaved = dh.account.onDeliciousLoveSaved;
	dh.account.deliciousEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.deliciousEntry, 'DELICIOUS');
	dh.account.deliciousEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.deliciousEntry, 'DELICIOUS');
}

dh.account.createTwitterEntry = function() {	
	dh.account.twitterEntry = new dh.lovehate.Entry('dhTwitter', 'Twitter username or profile URL', dh.account.initialTwitterName,
					'And *why* do I care what you\'re doing?', dh.account.initialTwitterHateQuip, 'Your friends see your Twitter updates.');
	dh.account.twitterEntry.onLoveSaved = dh.account.onTwitterLoveSaved;
	dh.account.twitterEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.twitterEntry, 'TWITTER');
	dh.account.twitterEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.twitterEntry, 'TWITTER');
}

dh.account.createGnomeTwitterEntry = function() {	
	dh.account.twitterEntry = new dh.love.Entry('dhTwitter', 'Twitter username or profile URL', dh.account.initialTwitterName);
	dh.account.twitterEntry.onLoveSaved = dh.account.onTwitterLoveSaved;
	dh.account.twitterEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.twitterEntry, 'TWITTER');
}

dh.account.createDiggEntry = function() {	
	dh.account.diggEntry = new dh.lovehate.Entry('dhDigg', 'Digg username or profile URL', dh.account.initialDiggName,
					'I don\'t dig it', dh.account.initialDiggHateQuip, 'Your friends get updates when you add diggs.');
	dh.account.diggEntry.onLoveSaved = dh.account.onDiggLoveSaved;
	dh.account.diggEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.diggEntry, 'DIGG');
	dh.account.diggEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.diggEntry, 'DIGG');
}

dh.account.createRedditEntry = function() {	
	dh.account.redditEntry = new dh.lovehate.Entry('dhReddit', 'Reddit username or profile URL', dh.account.initialRedditName,
					'Not reading it', dh.account.initialRedditHateQuip, 'Your friends get updates when you rate sites.');
	dh.account.redditEntry.onLoveSaved = dh.account.onRedditLoveSaved;
	dh.account.redditEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.redditEntry, 'REDDIT');
	dh.account.redditEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.redditEntry, 'REDDIT');
}

dh.account.createNetflixEntry = function() {	
	dh.account.netflixEntry = new dh.lovehate.Entry('dhNetflix', 'Netflix \u201CMovies At Home\u201D RSS feed URL', dh.account.initialNetflixUrl,
					'Movie rental stores are my daily respite', dh.account.initialNetflixHateQuip, 'Your friends get updates when you are sent new movies.',
					'http://www.netflix.com/RSSFeeds');
	dh.account.netflixEntry.setSpecialLoveValue("My Movies At Home");	
	dh.account.netflixEntry.onLoveSaved = dh.account.onNetflixLoveSaved;
	dh.account.netflixEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.netflixEntry, 'NETFLIX');
	dh.account.netflixEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.netflixEntry, 'NETFLIX');
}

dh.account.createGoogleReaderEntry = function() {	
	dh.account.googleReaderEntry = new dh.lovehate.Entry('dhGoogleReader', 'Google Reader public shared items page', dh.account.initialGoogleReaderUrl,
					"I don't like to read", dh.account.initialGoogleReaderHateQuip, 'Your friends see your Google Reader public shared items.',
					'http://www.google.com/reader/view');
	dh.account.googleReaderEntry.setSpecialLoveValue("My Shared Items");
	dh.account.googleReaderEntry.onLoveSaved = dh.account.onGoogleReaderLoveSaved;
	dh.account.googleReaderEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.googleReaderEntry, 'GOOGLE_READER');
	dh.account.googleReaderEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.googleReaderEntry, 'GOOGLE_READER');
}

dh.account.createPicasaEntry = function() {	
	dh.account.picasaEntry = new dh.lovehate.Entry('dhPicasa', 'Picasa username or public gallery URL', dh.account.initialPicasaName,
					'Pictures of cats', dh.account.initialPicasaHateQuip, 'Your friends see your public Picasa albums.');
	dh.account.picasaEntry.onLoveSaved = dh.account.onPicasaLoveSaved;
	dh.account.picasaEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.picasaEntry, 'PICASA');
	dh.account.picasaEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.picasaEntry, 'PICASA');
}

dh.account.createAmazonEntry = function() {	
	dh.account.amazonEntry = new dh.lovehate.Entry('dhAmazon', 'Amazon profile URL', dh.account.initialAmazonUrl,
					'I enjoy an ascetic lifestyle', dh.account.initialAmazonHateQuip, 
					'Your friends see what you add to your public wish lists and your reviews.',
					'http://www.amazon.com/gp/pdp/profile/');
	dh.account.amazonEntry.setSpecialLoveValue("My Profile");				
	dh.account.amazonEntry.onLoveSaved = dh.account.onAmazonLoveSaved;
	dh.account.amazonEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(dh.account.amazonEntry, 'AMAZON');
	dh.account.amazonEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(dh.account.amazonEntry, 'AMAZON');
}

dhAccountInit = function() {
	if (!dh.account.active) {
	    // we want to disable editing, but still display all the data we have
	    if (dh.util.exists('dhAccountContents'))
		    dh.dom.disableChildren(document.getElementById('dhAccountContents'));
		
		if (dh.util.exists('gnomeAccountContents'))    
		    dh.dom.disableChildren(document.getElementById('gnomeAccountContents'));
	}
	
	if (dh.util.exists('dhUsernameEntry')) {
	    var usernameEntry = new dh.formtable.ExpandableTextInput('dhUsernameEntry', "J. Doe");
	    usernameEntry.setDescription("The name you appear to others as.");
	    usernameEntry.setChangedPost('renameperson', 'name');
    }

    if (dh.util.exists('dhBioEntry')) {
	    var bioEntry = new dh.formtable.ExpandableTextInput('dhBioEntry', "I grew up in Kansas.");
	    bioEntry.setChangedPost('setbio', 'bio');
	}
	
	if (dh.util.exists('dhWebsiteEntry')) {
		var websiteEntry = new dh.formtable.ExpandableTextInput('dhWebsiteEntry', 'Your website URL');
		websiteEntry.setDescription("Your website will be linked from your Mugshot page.");
		websiteEntry.setChangedXmlMethod('setwebsiteaccount', 'url');
	}
	
	if (dh.util.exists('dhBlogEntry')) {
		var blogEntry = new dh.formtable.ExpandableTextInput('dhBlogEntry', 'Your blog URL');
		blogEntry.setDescription("Your friends will get updates when you post to your blog.")
		blogEntry.setChangedXmlMethod('setblogaccount', 'url');
	}
	
	if (dh.util.exists('dhPictureEntry')) {
	    // add some event handlers on the file input
	    dh.account.photoEntry = new dh.fileinput.Entry(document.getElementById('dhPictureEntry'));
	    // the div below could be null
	    dh.account.photoEntry.setBrowseButtonDiv(document.getElementById('dhStyledPictureEntry'));
	}
	
	if (dh.util.exists('dhEmailEntry')) {
	    // make pressing enter submit the email verify
	    var emailEntryNode = document.getElementById('dhEmailEntry');
	    emailEntryNode.onkeydown = function(ev) {
		    var key = dh.event.getKeyCode(ev);
		    if (key == ENTER) {
			    dh.account.verifyEmail();
		    }
	    }
	}
	
    dh.account.createImEntry();

	dh.photochooser.init("user", dh.account.userId)

	// We assume if one of these exists, they all exist. If the division between the
	// online.gnome.org and mugshot version of the account page changes, then this needs
	// to be adjusted
	if (dh.util.exists('dhMySpaceFormContainer')) {
	    dh.account.createMyspaceEntry();
		dh.account.createYouTubeEntry();
		dh.account.createLastFmEntry();
		dh.account.createFlickrEntry();
		dh.account.createLinkedInEntry();
		dh.account.createRhapsodyEntry();
		dh.account.createDeliciousEntry();
		dh.account.createTwitterEntry();
		dh.account.createDiggEntry();
		dh.account.createRedditEntry();
		dh.account.createNetflixEntry();
		dh.account.createGoogleReaderEntry();
		dh.account.createPicasaEntry();	
		dh.account.createAmazonEntry();
	} else {
	    // TODO: not sure yet how we'll figure out which entries to create, so hardcoding Twitter for now
		dh.account.createGnomeTwitterEntry();
	}	
}

dh.event.addPageLoadListener(dhAccountInit);
