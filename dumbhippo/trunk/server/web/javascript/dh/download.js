dojo.provide("dh.download")

dojo.require("dojo.render")
dojo.require("dh.server")

dh.download.updateDownload = function() {
	var acceptedTerms = document.getElementById("dhAcceptTerms").checked

	var className = acceptedTerms ? "dh-download-product" : "dh-download-product dh-download-product-disabled"
	document.getElementById("dhDownloadProduct").className = className
}

dh.download.doDownload = function(url) {
	if (dh.download.needTermsOfUse && !document.getElementById("dhAcceptTerms").checked)
		return

	window.open(url, "_self")
	if (dh.download.needTermsOfUse) {
		dh.server.doPOST("acceptterms",
						{},
						function(type, data, http) {
						},
						function(type, error, http) {
							alert("Oops! Error accepting the terms of use agreement");
						});
	}
}

dh.download.init = function() {
	document.getElementById("dhAcceptTerms").checked = false
	dh.download.updateDownload()
}
