dojo.provide("dh.download")

dojo.require("dojo.render")
dojo.require("dojo.html")
dojo.require("dh.server")

dh.download.updateDownload = function() {
	var acceptedTerms = document.getElementById("dhAcceptTerms").checked

	var className = acceptedTerms ? "dh-download-product" : "dh-download-product dh-download-product-disabled"
	document.getElementById("dhDownloadProduct").className = className
	
	var skipNode = document.getElementById('dhSkipDownload');
	if (acceptedTerms)
		dojo.html.removeClass(skipNode, "dh-download-product-disabled");
	else
		dojo.html.addClass(skipNode, "dh-download-product-disabled");	
}

dh.download.doDownload = function(url) {
	// be careful, right now "url" can be either the download binary or "/" (for "skip download")

	if (dh.download.needTermsOfUse && !document.getElementById("dhAcceptTerms").checked) {
		document.getElementById("dhAcceptTermsBox").className = "dh-accept-terms-box-warning"
		return
	}

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
	if (dh.download.needTermsOfUse) {
		document.getElementById("dhAcceptTerms").checked = false
		dh.download.updateDownload()
	}
}
