dojo.provide("dh.password");

dojo.require("dojo.event.*");
dojo.require("dojo.html");
dojo.require("dojo.string");
dojo.require("dh.util");
dojo.require("dh.server");

dh.password.passwordEntry = null;
dh.password.againEntry = null;
dh.password.setButton = null;

dhPasswordFormUpdate = function() {
	var first = dh.password.passwordEntry.value;
	var second = dh.password.againEntry.value;
	
	// keep spaces out of the entry
	first = dojo.string.trim(first);
	dh.password.passwordEntry.value = first;
	second = dojo.string.trim(second);
	dh.password.againEntry.value = second;
	
	if (first != second || first.length < 6) {
		dh.password.setButton.disabled = true;
	} else {
		dh.password.setButton.disabled = false;
	}
}

dh.password.inProgress = false;

dhPasswordFormSubmit = function() {
		
		var password = dojo.string.trim(dh.password.passwordEntry.value);
		
		if (dh.password.inProgress)
			return;

		dh.password.inProgress = true;
		dh.server.doPOST("setpassword",
						{ 
							"password" : password
						},
						function(type, data, http) {
							dh.password.passwordEntry.value = "";							
							dh.password.againEntry.value = "";
							dhPasswordFormUpdate();
													
							dh.password.inProgress = false;
						},
						function(type, error, http) {
							dh.password.inProgress = false;
							alert("failed to set your password... try again?");
						});
}

dhPasswordInit = function() {
	dh.password.passwordEntry = document.getElementById('dhPasswordEntry');
	dh.password.againEntry = document.getElementById('dhPasswordAgainEntry');
	dh.password.setButton = document.getElementById('dhSetPasswordButton');
	
	// this happens when your account is disabled
	if (!dh.password.passwordEntry)
		return;

	dojo.event.connect(dh.password.passwordEntry, "onchange", dj_global, "dhPasswordFormUpdate");
	dojo.event.connect(dh.password.againEntry, "onchange", dj_global, "dhPasswordFormUpdate");
	dojo.event.connect(dh.password.passwordEntry, "onkeyup", dj_global, "dhPasswordFormUpdate");
	dojo.event.connect(dh.password.againEntry, "onkeyup", dj_global, "dhPasswordFormUpdate");

	dojo.event.connect(dh.password.setButton, "onclick", dj_global, "dhPasswordFormSubmit");
	
	dhPasswordFormUpdate();
}

dojo.event.connect(dojo, "loaded", dj_global, "dhPasswordInit");
