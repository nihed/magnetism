dojo.provide("dh.groups")
dojo.require("dh.server")
dojo.require("dh.actions")

dh.groups._setControlsWorking = function (id, isWorking) {
	var controlDiv = document.getElementById("dhGroupInvitationControls-" + id)
	var workingDiv = document.getElementById("dhGroupInvitationWorking-" + id)
	controlDiv.style.display = isWorking ? "none" : "block";
	workingDiv.style.display = isWorking ? "block" : "none";
}

dh.groups.joinGroup = function (id) {
	dh.groups._setControlsWorking(id, true)
	dh.actions.joinGroup(id, function () {
		dh.groups._setControlsWorking(id, false)
		document.getElementById("dhGroupInvitationAccept-" + id).style.display = "none"
		document.getElementById("dhGroupInvitationAccepted-" + id).style.display = "block"
		document.getElementById("dhGroupInvitationDecline-" + id).style.display = "block"		
		document.getElementById("dhGroupInvitationDeclined-" + id).style.display = "none"		
	}, 
	function () {
	})
}

dh.groups.leaveGroup = function (id) {
	dh.groups._setControlsWorking(id, true)
	dh.actions.leaveGroup(id, function () {
		dh.groups._setControlsWorking(id, false)
		document.getElementById("dhGroupInvitationDecline-" + id).style.display = "none"
		document.getElementById("dhGroupInvitationDeclined-" + id).style.display = "block"
		document.getElementById("dhGroupInvitationAccept-" + id).style.display = "block"		
		document.getElementById("dhGroupInvitationAccepted-" + id).style.display = "none"
	}, 
	function () {
	})	
}
