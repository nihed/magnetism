dojo.provide("dh.myspace");

dojo.require("dojo.event.*");
dojo.require("dojo.html");
dojo.require("dojo.string");
dojo.require("dh.util");
dojo.require("dh.server");

dh.password.passwordEntry = null;
dh.password.againEntry = null;
dh.password.setButton = null;

dh.myspace.NameInput = function () {
	this.inputBox = document.getElementById("dhMySpaceName")
	this.submitButton = document.getElementById("dhMySpaceSubmit")
	this.description = document.getElementById("dhMySpaceDescription")
	this.changeDescription = document.getElementById("dhMySpaceChangeDescription")

	this.submitActive = false
	
	this.updateDescription = function () {	
		if (this.inputBox.value.length > 0) {
			this.changeDescription.style.display = "block";
			this.description.style.display = "none";		
		} else {
		this.changeDescription.style.display = "none";		
			this.description.style.display = "block";
		}
	}
	
	// And do it now
	this.updateDescription()

	this.setSubmitActive = function (isActive) {
		this.submitActive = isActive
		this.submitButton.disabled = isActive
	}

	this.submitHandler = function() {
		var name = dojo.string.trim(this.inputBox.value);
		
		if (name.length == 0) {
			alert("Please give your MySpace name")
			return
		}
		
		if (this.submitActive)
			return;

		var nameInput = this
		this.setSubmitActive(true);
		dh.server.doPOST("setmyspacename",
						{ 
							"name" : name
						},
						function(type, data, http) {
							nameInput.updateDescription()
							nameInput.setSubmitActive(false);
						},
						function(type, error, http) {
							nameInput.setSubmitActive(false);
							alert("Oops! Couldn't set your myspace name, please try again later");
						});
	}
	
	dojo.event.connect(this.submitButton, "onclick", this, "submitHandler")	
}

dh.myspace.instance = null;

var dhMySpaceInit = function() {
	if (!document.getElementById("dhMySpaceName"))
		return; // account is probably disabled so no myspace name box
	dh.myspace.instance = new dh.myspace.NameInput();
}

dojo.event.connect(dojo, "loaded", dhMySpaceInit);
