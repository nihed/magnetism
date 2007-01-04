dojo.provide('dh.feeds');
dojo.require('dojo.html');
dojo.require('dh.dom');

// Callbacks that are supposed to be filled in by the specific page with what to do on button presses
dh.feeds.loadingCancel = null;
dh.feeds.previewOK = null;
dh.feeds.previewCancel = null;
dh.feeds.failedTryAgain = null;
dh.feeds.failedCancel = null;

dh.feeds.setUrl = function(url) {
	var feedPopups = dojo.html.getElementsByClass('dh-feed-popup');
	var i = 0;
	while (i < feedPopups.length) {
		var urlNodes = dojo.html.getElementsByClass('dh-subtitle', feedPopups[i]);
		if (urlNodes.length != 1)
			throw "multiple subtitle elements in feed popup";
		dh.dom.textContent(urlNodes[0], url);
		i = i + 1;
	}	
}
