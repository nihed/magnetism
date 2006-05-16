dojo.provide("dh.download")

dojo.require("dojo.string")
dojo.require("dh.server")

dh.download.nameNow = false

dh.download.onNowSelected = function() {
	dh.download.nameNow = true
	dh.download.updateDownload()
}

dh.download.onLaterSelected = function() {
	dh.download.nameNow = false
	dh.download.updateDownload()
}

dh.download.updateDownload = function() {
	var mySpaceDownload = document.getElementById("dhMySpaceDownload")
	var mySpaceName = document.getElementById("dhMySpaceName")

	mySpaceName.disabled = !dh.download.nameNow
	mySpaceDownload.disabled = dh.download.nameNow && dojo.string.trim(mySpaceName.value).length == 0
}

dh.download.doDownload = function(url) {
	var mySpaceName = document.getElementById("dhMySpaceName")
	var name = dojo.string.trim(mySpaceName.value)

	window.open(url, "_self")	
	if (dh.download.nameNow) {
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

dh.download.init = function() {
	// this node only exists if coming from myspace stuff
	var myspaceNowNode = document.getElementById("dhMySpaceRadioNow")
	if (myspaceNowNode) {
		dh.download.nameNow = myspaceNowNode.checked
		dh.download.updateDownload()
	}
}

dojo.event.connect(dojo, "loaded", function () { dh.download.init() })
