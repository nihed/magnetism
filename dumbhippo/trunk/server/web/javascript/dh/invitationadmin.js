dojo.provide("dh.invitationadmin");

dojo.require("dh.server");
dojo.require("dh.util");

dh.invitationadmin.invite = function(countToInvite) {
    subject = document.getElementById("dhSubjectEntry")
    message = document.getElementById("dhMessageEntry")

   	dh.server.doPOST("invitewantsin",
				     { "countToInvite" : countToInvite,
				       "subject" : subject.value,
				       "message" : message.value },
		  	    	 function(type, data, http) {
                         document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     dh.util.showMessage("Couldn't invite");
		  	    	 });	  	    	   	 
    dh.util.showMessage("Processing...");
}