dojo.provide("dh.welcome")

dojo.require("dojo.string")
dojo.require("dh.server")

dh.welcome.nameNow = false

dh.welcome.onNowSelected = function() {
	dh.welcome.nameNow = true
	dh.welcome.updateDownload()
}

dh.welcome.onLaterSelected = function() {
	dh.welcome.nameNow = false
	dh.welcome.updateDownload()
}

dh.welcome.updateDownload = function() {
	var mySpaceDownload = document.getElementById("dhMySpaceDownload")
	var mySpaceName = document.getElementById("dhMySpaceName")

	mySpaceName.disabled = !dh.welcome.nameNow
	mySpaceDownload.disabled = dh.welcome.nameNow && dojo.string.trim(mySpaceName.value).length == 0
}

dh.welcome.doDownload = function(url) {
	var mySpaceName = document.getElementById("dhMySpaceName")
	var name = dojo.string.trim(mySpaceName.value)

	window.open(url, "_self")	
	if (dh.welcome.nameNow) {
		dh.server.doPOST("setmyspacename",
						{ 
							"name" : name
						},
						function(type, data, http) {
						},
						function(type, error, http) {
							alert("Oops! Couldn't set your myspace name, please try again later");
						});
	}
}

dh.welcome.init = function() {
	// this node only exists if coming from myspace stuff
	var myspaceNowNode = document.getElementById("dhMySpaceRadioNow")
	if (myspaceNowNode) {
		dh.welcome.nameNow = myspaceNowNode.checked
		dh.welcome.updateDownload()
	}
}

dojo.event.connect(dojo, "loaded", function () { dh.welcome.init() })
