dojo.provide("dh.login");

dojo.require("dh.server");
dojo.require("dh.util");

dh.login.sendSigninLink = function() {
	var form = document.getElementById("dhLoginNoPasswordForm")
   	dh.server.doPOST("sendloginlinkemail",
				     { "address" : form.address.value },
		  	    	 function(type, data, http) {
	  	    	 	 	var successDisplay = document.getElementById("dhLoginSuccessful");
	  	    	 	 	successDisplay.style.display = "block";
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	 	window.location.pathname = "/error";
		  	    	 });
	return false;
}