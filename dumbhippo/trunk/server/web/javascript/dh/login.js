dojo.provide("dh.login");

dojo.require("dh.server");
dojo.require("dh.util");
dojo.require("dh.dom");
dojo.require("dh.event");

dh.login.showingPassword = false;
dh.login.form = null;

dh.login.setNotification = function(msg) {
	var successDisplay = document.getElementById("dhLoginNotification");
  	successDisplay.style.display = "block";
  	dh.util.clearNode(successDisplay)
  	successDisplay.appendChild(document.createTextNode(msg))
}

dh.login.sendSigninLink = function() {
   	dh.server.doXmlMethod("sendloginlinkemail",
				     { "address" : dh.login.form.address.value },
		  	    	 function(childNodes, http) {
						dh.login.setNotification("Login link sent! Check your email.")
		  	    	 },
		  	    	 function(code, msg, http) {
		  	    	 	dh.login.setNotification(msg)
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	 	dh.login.setNotification("Unknown problem sending login link")
		  	    	 });
	return false;
}

dh.login.togglePasswordBox = function() {
	var passwordLabel = document.getElementById('dhLoginPasswordLabel');
	var passwordEntry = document.getElementById('dhLoginPasswordEntry');
	var passwordHelp = document.getElementById('dhPasswordHelp');
	var toggleLink = document.getElementById('dhLoginTogglePasswordLink');
	var showingEntry = document.getElementById('dhLoginPasswordShowing');
	var loginButton = document.getElementById('dhLoginButton');
	if (dh.login.showingPassword) {
		passwordLabel.style.display = 'none';
		passwordEntry.style.display = 'none';
		passwordHelp.style.display  = 'none';
		loginButton.value = "Mail me a log in link";
		dh.dom.textContent(toggleLink, "Log in with password");
		showingEntry.value = "false";
		dh.login.showingPassword = false;
	} else {
		passwordLabel.style.display = 'block';
		passwordEntry.style.display = 'inline';
		passwordHelp.style.display  = 'inline';
		loginButton.value = "Log in";
		dh.dom.textContent(toggleLink, "Don't know my password");
		showingEntry.value = "true";
		dh.login.showingPassword = true;
	}
}

dhLoginInit = function() {
	dh.login.showingPassword = true; // so it gets toggled to "off" by default
	dh.login.togglePasswordBox();
	// access key focuses the link but doesn't activate it, so we need this
	document.onkeydown = function(ev) {
		if ((dh.event.getKeyCode(ev) == 80 || dh.event.getKeyCode(ev) == 112) // 'P' or 'p'
		    && dh.event.getAltKey(ev)) {
		    dh.login.togglePasswordBox();
		    return false; // don't do something else with this key
		} else {
			return true;
		}
	}

	dh.login.form = document.getElementById('dhLoginForm');
	dh.login.form.onsubmit = function() {
		if (dh.login.showingPassword) {
			// we want to submit the form
			return true;
		} else {
			// we want to be all ajax-tastic
			dh.login.sendSigninLink();
			return false; // don't submit
		}
	}
	document.getElementById("dhLoginAddressEntry").focus();
}

dojo.event.connect(dojo, "loaded", dj_global, "dhLoginInit");
