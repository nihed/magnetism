trace("loading nowPlaying.as");

if (!baseUrl) {
	// debug base url and person
	baseUrl = "http://hp.debug.dumbhippo.com:8080";
	if (!who)
		who = "c4a3fc1f528070";
}

// A big rectangle on top, to catch clicks and let us do fade-in
createEmptyMovieClip("invisibleButton", 30);
var button:MovieClip = invisibleButton;
var width:Number = 440;
var height:Number = 120;
button.beginFill(0xFFFFFF, 100); // rgb 24-bit, alpha 0-100
button.lineTo(0, height);
button.lineTo(width, height);
button.lineTo(width, 0);
button.lineTo(0, 0);
button.onRelease = function() {
	trace("released");
	getURL(baseUrl + "/music?who=" + who, "_self");
}

var fadeInTime:Number = 1000;
var fadeInFrames:Number = 30;
var fadeInCount:Number = 0;
var startedFadeIn:Boolean = false;
var fadeInIntervalId:Number = null;

var updateFadeIn = function() {
	// we're fading in the .swf by fading out the button on top
	var newAlpha:Number = 100.0 - ((fadeInCount / fadeInFrames) * 100.0);
	button._alpha = newAlpha;
	trace("button._alpha = " + button._alpha);
	if (fadeInCount == fadeInFrames) {
		clearInterval(fadeInIntervalId);
		// leave it non-null so we never add it again
	} else {
		fadeInCount = fadeInCount + 1;
	}
}

var fadeIn = function() {
	if (fadeInIntervalId == null) {
		fadeInIntervalId = setInterval(updateFadeIn, fadeInTime / fadeInFrames);
	}
}

var clipNum:Number = 0;
var createNewMovie = function() {
	var clip:MovieClip = createEmptyMovieClip("holder_" + clipNum, 0);
	clipNum = clipNum + 1;
	clip._visible = false;
	clip.createEmptyMovieClip("albumArtInstance", 2);
	clip.createEmptyMovieClip("backgroundActiveInstance", 1);
	clip.createEmptyMovieClip("backgroundInactiveInstance", 0);

	clip.createTextField("online", 3, 20, 20, 30, 30);
	clip.createTextField("artist", 4, 40, 40, 30, 30);
	clip.createTextField("songTitle", 5, 60, 60, 30, 30);
	
	clip.defaultImages = { albumArt : baseUrl + "/images/no_image_available75x75light.gif",
							activeBackground : null,
							inactiveBackground : null };
	clip.imageTargets = { albumArt : clip.albumArtInstance,
							activeBackground : clip.backgroundActiveInstance,
							inactiveBackground : clip.backgroundInactiveInstance };
	clip.previousImages = {};
	
	clip.pendingLoadItems = 0;
	
	return clip;
}

var currentMovie:MovieClip = null;
var loadingMovie:MovieClip = null;

var checkSwap = function() {
	if (!loadingMovie)
		return;
	if (loadingMovie.pendingLoadItems > 0) {
		trace(loadingMovie.pendingLoadItems + " still pending");
		return;
	}
	trace(loadingMovie.pendingLoadItems + " pending items, swapping in");
	var toRemove:MovieClip = currentMovie;
	currentMovie = loadingMovie;
	loadingMovie = null;
	currentMovie._visible = true;
	if (toRemove) {
		toRemove._visible = false;
		toRemove.removeMovieClip();
	}
	fadeIn(); // has no effect if it's already been done
}

// null fullUrl = load default
var loadImage = function(clip:MovieClip, which:String, fullUrl:String, depth:Number) {
	if (!fullUrl)
		fullUrl = clip.defaultImages[which];
	if (!fullUrl) {
		trace("nothing to load");
		return;
	}
	if (arguments.length < 3)
		depth = 1
	if (depth > 2)
		return; // give up
	var previousUrl = clip.previousImages[which];
	if (fullUrl == previousUrl) {
		trace("already loaded " + previousUrl);
		return;
	}

	trace("loading " + fullUrl + " previous was " + previousUrl);
	var loader:MovieClipLoader = new MovieClipLoader();
	var listener:Object = new Object();
	listener.onLoadError = function(targetClip:MovieClip, errorCode:String) {
		// note, we could be replaced as the loadingMovie already
		trace("failed to load " + fullUrl);
		loadImage(clip, which, null, depth + 1);
		clip.pendingLoadItems = clip.pendingLoadItems - 1;
		checkSwap();
	}
	listener.onLoadComplete = function(targetClip:MovieClip) {
		// note, we could be replaced as the loadingMovie already
		trace("completed load of " + fullUrl + " to " + targetClip);
		clip.previousImages[which] = fullUrl;
		clip.pendingLoadItems = clip.pendingLoadItems - 1;
		checkSwap();
	}
	loader.addListener(listener);
	 // returns false if failed to _send_ request
	if (!loader.loadClip(fullUrl, clip.imageTargets[which])) {
		trace("failed to send request for url " + fullUrl);
	}
	clip.pendingLoadItems = clip.pendingLoadItems + 1;
	trace("pending " + clip.pendingLoadItems);
}

var getNodeContent = function(node:XMLNode) {
	if (node.nodeType == 3)
		return node.nodeValue;
	
	var s:String = "";	
	for (var i = 0; i < node.childNodes.length; ++i) {
		var child:XMLNode = node.childNodes[i];
		s = s + getNodeContent(child);
	}
	return s;
}

var setStillPlaying = function(clip:MovieClip, stillPlaying:Boolean) {
	if (stillPlaying) {
		if (clip.backgroundActiveInstance.getDepth() < clip.backgroundInactiveInstance.getDepth())
			clip.backgroundActiveInstance.swapDepths(clip.backgroundInactiveInstance);
		clip.online.text = "";
	} else {
		if (clip.backgroundInactiveInstance.getDepth() < clip.backgroundActiveInstance.getDepth())
			clip.backgroundInactiveInstance.swapDepths(clip.backgroundActiveInstance);	
		clip.online.text = "Music stopped";
	}
}

var parseSong = function(clip:MovieClip, songNode:XMLNode) {
	var gotImage:Boolean = false;
	for (var i = 0; i < songNode.childNodes.length; ++i) {
		var node:XMLNode = songNode.childNodes[i];
		if (node.nodeName == "songTitle")
			clip.songTitle.text = getNodeContent(node);
		else if (node.nodeName == "artist")
			clip.artist.text = getNodeContent(node);
		else if (node.nodeName == "stillPlaying") {
			if (forceMode) {
				trace("forcing mode to " + forceMode);
				if (forceMode == "active")
					setStillPlaying(true);
				else
					setStillPlaying(false);
			} else {
				var stillPlayingStr:String = getNodeContent(node);
				var stillPlaying = stillPlayingStr && (stillPlayingStr == "true");
				setStillPlaying(clip, stillPlaying);
			}
		} else if (node.nodeName == "image") {
			var fullUrl:String = getNodeContent(node);
			loadImage(clip, "albumArt", fullUrl);
			gotImage = true;
		}
	}
	if (!gotImage)
		loadImage(clip, "albumArt", null);
}

var parseTheme = function(clip:MovieClip, themeNode:XMLNode) {
	for (var i = 0; i < themeNode.childNodes.length; ++i) {
		var node:XMLNode = themeNode.childNodes[i];
		
		if (node.nodeName == "activeImageUrl") {
			loadImage(clip, 'activeBackground', baseUrl + getNodeContent(node));
		} else if (node.nodeName == "inactiveImageUrl") {
			loadImage(clip, 'inactiveBackground', baseUrl + getNodeContent(node));
		} else if (node.nodeName == "text") {
			// FIXME
		} else if (node.nodeName == "albumArt") {
			// FIXME
		}
	}
}

var songUpdateCount:Number = 0;

var updateSong = function() {
	
	songUpdateCount = songUpdateCount + 1;
	
	if (songUpdateCount > 2) // FIXME a larger number
		return;
	
	if (loadingMovie) {
		loadingMovie.removeMovieClip();
		loadingMovie = null;
	}
	loadingMovie = createNewMovie();
	var clip:MovieClip = loadingMovie;
	
	var meuXML:XML = new XML();
	meuXML.ignoreWhite = true;
	meuXML.onLoad = function(success:Boolean) {
		if (!success) {
			return;
		}
		if (clip != loadingMovie) {
			trace("we aren't the loadingMovie anymore, not parsing xml");
			return; // just get garbage collected
		}
		var root:XML = this.childNodes[0];
		for (var i = 0; i < root.childNodes.length; ++i) {
			var node:XMLNode = root.childNodes[i];
			if (node.nodeName == "song")
				parseSong(clip, node);
			else if (node.nodeName == "theme")
				parseTheme(clip, node);
		}
	};
	var reqUrl = baseUrl + "/xml/nowplaying?who=" + who;
	if (theme)
		reqUrl = reqUrl + "&theme=" + theme;
	meuXML.load(reqUrl);
};

// once per minute
setInterval(updateSong, 1000*3); // FIXME put back

// update once on load
updateSong();
