dojo.provide("dh.admin");

dojo.require("dh.server");

dh.admin.sendRepairEmail = function(userId) {
   	dh.server.doPOST("sendrepairemail",
				     { "userId" : userId },
		  	    	 function(type, data, http) {
		  	    	     alert("Repair email sent");
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't send repair email");
		  	    	 });
}

dh.admin.reindexAll = function() {
   	dh.server.doPOST("reindexall",
				     {},
		  	    	 function(type, data, http) {
		  	    	     alert("Succesfully reindexed everything");
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Failure reindexing");
		  	    	 });
}
