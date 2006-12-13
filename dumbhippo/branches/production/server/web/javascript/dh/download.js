dojo.provide("dh.download")

dojo.require("dojo.html")
dojo.require("dh.server")

dh.download.getImage = function(node) {
	if (node.nodeName.toLowerCase() == "img")
		return node;
	for (var i = 0; i < node.childNodes.length; i++) {
		var child = node.childNodes.item(i);
		if (child.nodeName.toLowerCase() == "img")
			return child;
	}
	return null;
}

dh.download.updateDownload = function() {
	var acceptedTerms = document.getElementById("dhAcceptTerms").checked

	var className = acceptedTerms ? "dh-download-product" : "dh-download-product dh-download-product-disabled"
	var downloadNode = document.getElementById("dhDownloadProduct");
	
	// it's null when we don't have a download to offer
	if (downloadNode) {
		downloadNode.className = className;
	
		var img = dh.download.getImage(downloadNode);
		if (img) {
			if (acceptedTerms)
				img.src = dhImageRoot3 + "download_now_button.gif";
			else
				img.src = dhImageRoot3 + "download_now_disabled.gif";
		}
	}
	
	// this can return a link with either text or an image in it
	var skipNode = document.getElementById('dhSkipDownload');
	var skipNodeImg = dh.download.getImage(skipNode);
	
	if (acceptedTerms) {
		skipNodeImg.src = dhImageRoot3 + "no_thanks_button.gif";
		dojo.html.removeClass(skipNode, "dh-download-product-disabled");
	} else {
		skipNodeImg.src = dhImageRoot3 + "no_thanks_disabled.gif";
		dojo.html.addClass(skipNode, "dh-download-product-disabled");	
	}
}

dh.download.doDownload = function(url) {
	// "url" should be the download binary; if it's empty, it means we are skipping the download.
	// Whether we download or not, we proceed to the account page.

	if (dh.download.needTermsOfUse && !document.getElementById("dhAcceptTerms").checked) {
		document.getElementById("dhAcceptTermsBox").className = "dh-accept-terms-box-warning";
		return;
	}

	// the download must begin in an onclick handler or we'll get the yellow stripe blocker
    if (url)
	    window.open(url, "_self");
	
	if (dh.download.needTermsOfUse) {
		dh.server.doPOST("acceptterms",
						{},
						function(type, data, http) {
							// the async redirect to /account can work even if we downloaded, but there's 
							// a race if we window.open the account page before the download starts, 
							// so we improve our chances with this timeout.
							// A larger-scope reworking of the login page could avoid the problem.
							setTimeout('window.open("/account?fromDownload=true", "_self");', 7000);
						},
						function(type, error, http) {
							alert("Oops! Error accepting the terms of use agreement");
						});
	} else if (!url) {
		// this is only done if we skipped download, otherwise it seems to override
		// the earlier window.open and prevent the download
        window.open("/account?fromDownload=true", "_self");
	}
}

dh.download.init = function() {
	if (dh.download.needTermsOfUse) {
		document.getElementById("dhAcceptTerms").checked = false;
		dh.download.updateDownload();
	}
}
