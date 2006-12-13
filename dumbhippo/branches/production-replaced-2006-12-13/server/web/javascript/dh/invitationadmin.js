dojo.provide("dh.invitationadmin");

dojo.require("dh.server");
dojo.require("dh.util");

dh.invitationadmin.invite = function(countToInvite) {
    subject = document.getElementById("dhSubjectEntry")
    message = document.getElementById("dhMessageEntry")
    
    var allInputs = document.getElementsByTagName("input")
    var suggestedGroups = []
    
    for (var i = 0; i < allInputs.length; ++i) {
        var n = allInputs[i]
        if (n.type == "checkbox" && n.checked) {
            suggestedGroups.push(n.value)
        }
    }    
        
    var commaSuggestedGroups = dh.util.join(suggestedGroups, ",");    
        
   	dh.server.doPOST("invitewantsin",
				     { "countToInvite" : countToInvite,
				       "subject" : subject.value,
				       "message" : message.value,
				       "suggestedGroupIds" : commaSuggestedGroups },
		  	    	 function(type, data, http) {
                         document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     dh.util.showMessage("Couldn't invite");
		  	    	 });	  	    	   	 
    dh.util.showMessage("Processing...");
}