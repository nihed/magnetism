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


dh.account.hateQuipsArray = [];
dh.account.hateQuipsArray['myspace'] = 'I despise Tom and his space';
dh.account.hateQuipsArray['youtube'] = 'Video should kill the internet geeks';
dh.account.hateQuipsArray['flickr'] = 'Flickr doesn\'t do it for me';
dh.account.hateQuipsArray['linkedin'] = 'LinkedIn is for nerds';
dh.account.hateQuipsArray['rhapsody_rss'] = 'All-you-can-eat music services hurt my diet';
dh.account.hateQuipsArray['lastfm'] = 'Uhh...what\'s Last.fm?';
dh.account.hateQuipsArray['delicious'] = 'del.icio.us isn\'t';
dh.account.hateQuipsArray['twitter'] = 'And *why* do I care what you\'re doing?';
dh.account.hateQuipsArray['digg'] = 'I don\'t dig it';
dh.account.hateQuipsArray['reddit'] = 'Not reading it';
dh.account.hateQuipsArray['netflix_rss'] = 'Movie rental stores are my daily respite';
dh.account.hateQuipsArray['google_reader_rss'] = 'I don\'t like to read';
dh.account.hateQuipsArray['picasa'] = 'Pictures of cats';
dh.account.hateQuipsArray['amazon'] = 'I enjoy an ascetic lifestyle';
    
dh.account.whatWillHappenArray = [];
dh.account.whatWillHappenArray['myspace'] = 'Your friends get updates when you post to your MySpace blog.';
dh.account.whatWillHappenArray['youtube'] = 'Your friends get updates when you upload new videos.';
dh.account.whatWillHappenArray['flickr'] = 'Your friends get updates when you upload new photos and photo sets.';
dh.account.whatWillHappenArray['linkedin'] = '';
dh.account.whatWillHappenArray['rhapsody_rss'] = 'Your friends will see updates from your Rhapsody playlist.';
dh.account.whatWillHappenArray['lastfm'] = 'Your friends see what music you\'re listening to.';
dh.account.whatWillHappenArray['delicious'] = 'Your friends get updates when you add public bookmarks.';
dh.account.whatWillHappenArray['twitter'] = 'Your friends see your Twitter updates.';
dh.account.whatWillHappenArray['digg'] = 'Your friends get updates when you add diggs.';
dh.account.whatWillHappenArray['reddit'] = 'Your friends get updates when you rate sites.';
dh.account.whatWillHappenArray['netflix_rss'] = 'Your friends get updates when you are sent new movies.';
dh.account.whatWillHappenArray['google_reader_rss'] = 'Your friends see your Google Reader public shared items.';
dh.account.whatWillHappenArray['picasa'] = 'Your friends see your public Picasa albums.';
dh.account.whatWillHappenArray['amazon'] = 'Your friends see what you add to your public wish lists and your reviews.';
        
dh.account.helpLinkArray = [];
dh.account.helpLinkArray['rhapsody_rss'] = 'http://www.rhapsody.com/myrhapsody/rss.html';
dh.account.helpLinkArray['netflix_rss'] = 'http://www.netflix.com/RSSFeeds';
dh.account.helpLinkArray['google_reader_rss'] = 'http://www.google.com/reader/view';
dh.account.helpLinkArray['amazon'] ='http://www.amazon.com/gp/pdp/profile/';
        
dh.account.specialLoveValuesArray = [];
dh.account.specialLoveValuesArray['rhapsody_rss'] = 'My Recently Played Tracks'; 
dh.account.specialLoveValuesArray['netflix_rss'] = 'My Movies At Home';
dh.account.specialLoveValuesArray['google_reader_rss'] = 'My Shared Items';
dh.account.specialLoveValuesArray['amazon'] = 'My Profile';
   
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

dh.account.removeExternalAccount = function(id, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("removeexternalaccount",
				     { "id" : id },
						loadFunc, errorFunc);
}
dh.account.findFlickrAccount = function(email, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("findflickraccount",
				     { "email" : email },
						loadFunc, errorFunc);
}

dh.account.setFlickrAccount = function(id, nsid, email, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setflickraccount",
				     { "id" : id, "nsid" : nsid, "email" : email },
				     	loadFunc, errorFunc);
}
dh.account.setLinkedInProfile = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setlinkedinaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setMyspaceName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setmyspaceaccount",
				     { "id" : id, "name" : name },
						loadFunc, errorFunc);
}
dh.account.setYouTubeName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setyoutubeaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setLastFmName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setlastfmaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setDeliciousName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setdeliciousaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setTwitterName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("settwitteraccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setDiggName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setdiggaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setRedditName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setredditaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}
dh.account.setRhapsodyUrl = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setrhapsodyaccount",
   	                      { "id" : id, "url" : name },
   	                      loadFunc, errorFunc);
}
dh.account.setNetflixUrl = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setnetflixaccount",
   	                      { "id" : id, "url" : name },
   	                      loadFunc, errorFunc);
}
 
dh.account.setGoogleReaderUrl = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setgooglereaderaccount",
   	                      { "id" : id, "url" : name },
   	                      loadFunc, errorFunc);
}
dh.account.setPicasaName = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setpicasaaccount",
				     { "id" : id, "urlOrName" : name },
						loadFunc, errorFunc);
}

dh.account.setAmazonUrl = function(id, name, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setamazonaccount",
				          { "id" : id, "urlOrUserId" : name },
						  loadFunc, errorFunc);
}

dh.account.setOnlineAccountValue = function(type, id, value, loadFunc, errorFunc) {
   	dh.server.doXmlMethod("setonlineaccountvalue",
				          { "type" : type, "id" : id, "value" : value },
						  loadFunc, errorFunc);
}

dh.account.createExternalAccountOnLoveSavedFunc = function(entry, accountType, id, mugshotEnabled) {
	return function(value) {
	    switch(accountType) {
		case 'myspace':
		    dh.account.onMyspaceLoveSaved(entry, id, value);
		    break;
        case 'youtube':
            dh.account.onYouTubeLoveSaved(entry, id, value);
		    break;
        case 'flickr':
            dh.account.onFlickrLoveSaved(entry, id, value);
            break;
        case 'linkedin':
            dh.account.onLinkedInLoveSaved(entry, id, value);
		    break;
        case 'rhapsody_rss':
            dh.account.onRhapsodyLoveSaved(entry, id, value);
		    break;
        case 'lastfm':
            dh.account.onLastFmLoveSaved(entry, id, value);
		    break;
        case 'delicious':
            dh.account.onDeliciousLoveSaved(entry, id, value);
            break;
        case 'twitter':
            dh.account.onTwitterLoveSaved(entry, id, value);
            break;
        case 'digg':
            dh.account.onDiggLoveSaved(entry, id, value);
		    break;            
        case 'reddit':
            dh.account.onRedditLoveSaved(entry, id, value);
		    break;            
        case 'netflix_rss':
            dh.account.onNetflixLoveSaved(entry, id, value);
		    break;            
        case 'google_reader_rss':
            dh.account.onGoogleReaderLoveSaved(entry, id, value);
		    break;            
        case 'picasa':
            dh.account.onPicasaLoveSaved(entry, id, value);
		    break;            
        case 'amazon':
            dh.account.onAmazonLoveSaved(entry, id, value);
            break;
		default:
			dh.account.onLoveSaved(entry, accountType, id, value);
		}
	}
}

dh.account.createExternalAccountOnHateSavedFunc = function(entry, accountType, id, mugshotEnabled) {
	return function(value) {
		var oldMode = entry.getMode();
		entry.setBusy();
		dh.account.hateExternalAccount(accountType, value,
		 	    	 function(childNodes, http) {
		 	    	     // if we leave entry mode as "busy" we get weird problems with css styling,
		 	    	     // so we set it to the new mode in all cases 
		 	    	     entry.setMode('hate'); 
		 	    	     if (entry.getInitialMode() == "love") { 
		 	    	         // There is also no need to reload if the account was already hated 
		 	    	         // or the user was indifferent to it because we don't allow adding more accounts
		 	    	         // if you hate one of the type.
		 	    	 	     // But if you hate an account that you used to love, we want to make sure you are not
		 	    	 	     // able to add other accounts of this type, so we need to refresh.
		 	    	         dh.util.refresh();    
		 	             }
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	alert(msg);
		  	    	 	entry.setMode(oldMode);
		  	    	 });
	}
}

dh.account.createExternalAccountOnCanceledFunc = function(entry, accountType, id, mugshotEnabled) {
	return function(value) {
		var oldMode = entry.getMode();
		entry.setBusy();
		dh.account.removeExternalAccount(id, 
		 	    	 function(childNodes, http) {
		 	    	     entry.setMode('indifferent'); 
		 	    	     if (entry.getInitialMode() == "love") {
		 	    	         dh.util.refresh();    
		 	             }  
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	 alert(msg);
		  	    	 	 entry.setMode(oldMode);
		  	    	 });
	}
}

dh.account.onFlickrLoveSaved = function(entry, id, value) {
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
				
				dh.account.setFlickrAccount(id, nsid, value,
					function(childNodes, http) {
					     entry.setMode('love');
					     if (entry.getInitialMode() != "love") {
					         dh.util.refresh();  
		 	    	 	 }
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

dh.account.onMyspaceLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setMyspaceName(id, value, 
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
	 	    	     if (entry.getInitialMode() != "love") {	 	    	          
					     dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	 alert(msg);
	  	    	 	 entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onYouTubeLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setYouTubeName(id, value, 
	 	    	 function(childNodes, http) {
	 	    	     entry.setMode('love');
	 	    	     if (entry.getInitialMode() != "love") { 	    	           
				         dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLastFmLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setLastFmName(id, value, 
	 	    	 function(childNodes, http) {
	 	    	     entry.setMode('love');
	 	             if (entry.getInitialMode() != "love") {
			             dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg); // entry.setError(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLinkedInLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setLinkedInProfile(id, value,
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
	 	    	     if (entry.getInitialMode() != "love") {
					     dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onRhapsodyLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setRhapsodyUrl(id, value, 
	 	    	 function(childNodes, http) {
	 	    	     entry.setMode('love');  
					 if (entry.getInitialMode() != "love") {
				         dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onDeliciousLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setDeliciousName(id, value,
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
					 if (entry.getInitialMode() != "love") {  
					    dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onTwitterLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setTwitterName(id, value,
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
					if (entry.getInitialMode() != "love") {
				        dh.util.refresh();  
		 	    	} 	    	 		 	    	 	
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onDiggLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setDiggName(id, value,
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
	 	    	 	 if (entry.getInitialMode() != "love") {
				         dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onRedditLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setRedditName(id, value,
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
					if (entry.getInitialMode() != "love") {
				        dh.util.refresh();  
		 	    	}
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onNetflixLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setNetflixUrl(id, value, 
	 	    	 function(childNodes, http) {
	 	    	     entry.setMode('love'); 
					 if (entry.getInitialMode() != "love") {
				         dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onGoogleReaderLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setGoogleReaderUrl(id, value, 
	 	    	 function(childNodes, http) {
	 	    	     entry.setMode('love'); 
					 if (entry.getInitialMode() != "love") {		      
				         dh.util.refresh();  
		 	    	 }
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onPicasaLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setPicasaName(id, value,
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
					 if (entry.getInitialMode() != "love") { 
				         dh.util.refresh();  
		 	    	 }	 	    	 	
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onAmazonLoveSaved = function(entry, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setAmazonUrl(id, value, 
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
                                        linkElement.target = "_blank";				            
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
	 	    	 	
	 	    	 	if (entry.getInitialMode() != "love") {
				        dh.util.refresh();  
		 	    	}
	  	    	 },
	  	    	 function(code, msg, http) {
	  	    	 	alert(msg);
	  	    	 	entry.setMode(oldMode);
	  	    	 }); 
}

dh.account.onLoveSaved = function(entry, type, id, value) {
	var oldMode = entry.getMode();
	entry.setBusy();
  	dh.account.setOnlineAccountValue(type, id, value, 
	 	    	 function(childNodes, http) {
	 	    	     entry.setMode('love'); 
	 	    	     if (entry.getInitialMode() != "love") {
				         dh.util.refresh();  
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

    dh.account.onlineAccountEntries = [];
    
    if (dh.account.dhMugshotEnabledFlags != null) {
	    for (var i = 0; i < dh.account.dhNames.length; ++i) {
	        if (dh.account.dhNames[i] == "facebook")
	            continue;  
	        	        
	        var onlineAccountEntry = null;

	        if (dh.account.dhHateAllowedFlags[i]) {    
	            onlineAccountEntry = new dh.lovehate.Entry('dh' + dh.account.dhDomIds[i], dh.account.dhUserInfoTypes[i], dh.account.dhValues[i],
							                               dh.account.hateQuipsArray[dh.account.dhNames[i]], dh.account.dhHateQuips[i], dh.account.whatWillHappenArray[dh.account.dhNames[i]], 
							                               dh.account.helpLinkArray[dh.account.dhNames[i]]);
               	onlineAccountEntry.onHateSaved = dh.account.createExternalAccountOnHateSavedFunc(onlineAccountEntry,  dh.account.dhNames[i], dh.account.dhIds[i], dh.account.dhMugshotEnabledFlags[i]);
            } else {
	            onlineAccountEntry = new dh.love.Entry('dh' + dh.account.dhDomIds[i], dh.account.dhUserInfoTypes[i], dh.account.dhValues[i],
	                                                   dh.account.whatWillHappenArray[dh.account.dhNames[i]], dh.account.helpLinkArray[dh.account.dhNames[i]]);            
            }   		
				
		    if (dh.account.specialLoveValuesArray[dh.account.dhNames[i]] != null)
		        onlineAccountEntry.setSpecialLoveValue(dh.account.specialLoveValuesArray[dh.account.dhNames[i]]);
		    					                                                                                                                             
	        onlineAccountEntry.onLoveSaved = dh.account.createExternalAccountOnLoveSavedFunc(onlineAccountEntry, dh.account.dhNames[i], dh.account.dhIds[i], dh.account.dhMugshotEnabledFlags[i]);
	        onlineAccountEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(onlineAccountEntry, dh.account.dhNames[i], dh.account.dhIds[i], dh.account.dhMugshotEnabledFlags[i]);
            dh.account.onlineAccountEntries.push(onlineAccountEntry)
        }  
    } else {	    
	    for (var i = 0; i < dh.account.dhNames.length; ++i) {
	        var onlineAccountEntry = new dh.love.Entry('dh' + dh.account.dhDomIds[i], dh.account.dhUserInfoTypes[i], dh.account.dhValues[i]);
	        onlineAccountEntry.onLoveSaved = dh.account.createExternalAccountOnLoveSavedFunc(onlineAccountEntry, dh.account.dhNames[i], dh.account.dhIds[i], false);
	        onlineAccountEntry.onCanceled = dh.account.createExternalAccountOnCanceledFunc(onlineAccountEntry, dh.account.dhNames[i], dh.account.dhIds[i], false);
            dh.account.onlineAccountEntries.push(onlineAccountEntry);
        }
    }
       
	dh.photochooser.init("user", dh.account.userId)
	
}

dh.event.addPageLoadListener(dhAccountInit);
