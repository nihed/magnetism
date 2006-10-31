dojo.provide("dh.asyncActionLink")

dh.asyncActionLink.exec = function (gid, ctrlId, execCb) {
	var link = document.getElementById("dhActionExecLink-" + ctrlId)
	link.style.display = "none"
	var working = document.getElementById("dhActionWorking-" + ctrlId)
	working.style.display = "block"	
	execCb();
}

dh.asyncActionLink.complete = function (ctrlId) {
	var working = document.getElementById("dhActionWorking-" + ctrlId)
	working.style.display = "none"
	var complete = document.getElementById("dhActionComplete-" + ctrlId)
	complete.style.display = "block"	
}