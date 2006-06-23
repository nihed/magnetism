dojo.provide("dh.login");

dojo.require("dh.server");
dojo.require("dh.util");

dh.login.setNotification = function(msg) {
	var successDisplay = document.getElementById("dhLoginNotification");
  	successDisplay.style.display = "block";
  	dh.util.clearNode(successDisplay)
  	successDisplay.appendChild(document.createTextNode(msg))
}

dh.login.sendSigninLink = function() {
	var form = document.getElementById("dhLoginNoPasswordForm")	
   	dh.server.doXmlMethod("sendloginlinkemail",
				     { "address" : form.address.value },
		  	    	 function(childNodes, http) {
						dh.login.setNotification("Login link sent! Check your email or AIM.")
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	dh.login.setNotification(msg)
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	 	dh.login.setNotification("Unknown problem sending login link")
		  	    	 });
	return false;
}