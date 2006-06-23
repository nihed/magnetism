dojo.provide('dh.feeds');

// Callbacks that are supposed to be filled in by the specific page with what to do on button presses
dh.feeds.loadingCancel = null;
dh.feeds.previewOK = null;
dh.feeds.previewCancel = null;
dh.feeds.failedTryAgain = null;
dh.feeds.failedCancel = null;

dh.feeds.setUrl = function(url) {
	// FIXME fill in the feed being loaded in the subtitle of each popup
}
