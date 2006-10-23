dojo.provide("dh.download")

dojo.require("dojo.render")
dojo.require("dojo.html")
dojo.require("dh.server")

dh.download.updateDownload = function() {
	var acceptedTerms = document.getElementById("dhAcceptTerms").checked

	var className = acceptedTerms ? "dh-download-product" : "dh-download-product dh-download-product-disabled"
	var downloadNode = document.getElementById("dhDownloadProduct");
	
	if (downloadNode) // it's null when we don't have a download to offer
		downloadNode.className = className;
	
	var skipNode = document.getElementById('dhSkipDownload');
	
	if (acceptedTerms)
		dojo.html.removeClass(skipNode, "dh-download-product-disabled");
	else
		dojo.html.addClass(skipNode, "dh-download-product-disabled");	
}

dh.download.doDownload = function(url) {
	// "url" should be the download binary; if it's empty, it means we are skipping the download

	if (dh.download.needTermsOfUse && !document.getElementById("dhAcceptTerms").checked) {
		document.getElementById("dhAcceptTermsBox").className = "dh-accept-terms-box-warning"
		return;
	}

    // they can start downloading while the terms of use are being accepted
    if (url)
	    window.open(url, "_self")
	
	if (dh.download.needTermsOfUse) {
		dh.server.doPOST("acceptterms",
						{},
						function(type, data, http) {
						    if (!url)
	                            window.open("/account", "_self")
						},
						function(type, error, http) {
							alert("Oops! Error accepting the terms of use agreement");
						});
	} else if (!url) {
        window.open("/account", "_self")	
	}
}

dh.download.init = function() {
	if (dh.download.needTermsOfUse) {
		document.getElementById("dhAcceptTerms").checked = false
		dh.download.updateDownload()
	}
}
