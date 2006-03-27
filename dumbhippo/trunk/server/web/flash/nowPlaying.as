trace("loading nowPlaying.as");

if (!baseUrl) {
	// debug base url and person
	baseUrl = "http://hp.debug.dumbhippo.com:8080";
	if (!who)
		who = "c4a3fc1f528070";
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

/////////////////// "model" data types
// (not bothering to make "real" javascript objects, who wants to fight js object system)

var parseTheme = function(themeNode:XMLNode) {
	var theme:Object = {};
	for (var i = 0; i < themeNode.childNodes.length; ++i) {
		var node:XMLNode = themeNode.childNodes[i];
		if (node.nodeName == "activeImageUrl") {
			theme.activeImageUrl = getNodeContent(node);
		} else if (node.nodeName == "inactiveImageUrl") {
			theme.inactiveImageUrl = getNodeContent(node);
		} else if (node.nodeName == "text") {
			// FIXME
		} else if (node.nodeName == "albumArt") {
			// FIXME
		}
	}
	return theme;
}

var parseSong = function(songNode:XMLNode) {
	var song:Object = {};
	for (var i = 0; i < songNode.childNodes.length; ++i) {
		var node:XMLNode = songNode.childNodes[i];
		if (node.nodeName == "songTitle")
			song.title = getNodeContent(node);
		else if (node.nodeName == "artist")
			song.artist = getNodeContent(node);
		else if (node.nodeName == "stillPlaying") {
			if (forceMode) {
				trace("forcing mode to " + forceMode);
				if (forceMode == "active")
					song.stillPlaying = true;
				else
					song.stillPlaying = false;
			} else {
				var stillPlayingStr:String = getNodeContent(node);
				var stillPlaying = stillPlayingStr && (stillPlayingStr == "true");
				song.stillPlaying = stillPlaying;
			}
		} else if (node.nodeName == "image") {
			var fullUrl:String = getNodeContent(node);
			song.albumArtUrl = fullUrl;
		}
	}
	return song;
}

// this is kinda bogus (e.g. won't work right if the two objects have different fields)
var genericEquals = function(objA, objB) {
	if (objA == objB)
		return true;
	if (objA == null && objB != null)
		return false;
	else if (objA != null && objB == null)
		return false;
	else if (objA == undefined && objB != undefined)
		return false;
	else if (objA != undefined && objB == undefined)
		return false;
		
	for (var i in objA) {
		if (objA[i] != objB[i])
			return false;
	}
	return true;
}

var songEquals = genericEquals;
var themeEquals = genericEquals;



/////////////////// "view" movie stuff

var rootMovie = createEmptyMovieClip("rootMovie", 0);

var currentMovie:MovieClip = null;
var loadingMovie:MovieClip = null;

var removeMovie = function(clip:MovieClip) {
	trace("-------- removing clip: " + clip._name);
	if (clip == currentMovie)
		currentMovie = null;
	if (clip == loadingMovie)
		loadingMovie = null;
	clip.removeMovieClip();
}

// A big rectangle on top, to catch clicks
createEmptyMovieClip("invisibleButton", 30);
var button:MovieClip = invisibleButton;
var width:Number = 440;
var height:Number = 120;
button.beginFill(0xFFFFFF, 0); // rgb 24-bit, alpha 0-100
button.lineTo(0, height);
button.lineTo(width, height);
button.lineTo(width, 0);
button.lineTo(0, 0);
button.onRelease = function() {
	trace("released");
	getURL(baseUrl + "/music?who=" + who, "_self");
}

var fadeTime:Number = 1000;
var fadeFrames:Number = 30;

var stopFade = function(clip:MovieClip) {
	if (!clip.fadeIntervalId)
		return;
	trace("removing fade interval " + clip.fadeIntervalId);
	clearInterval(clip.fadeIntervalId);
	clip.fadeIntervalId = null;
	if (clip.removeAfterFade) {
		removeMovie(clip);
	} else {
		trace("not removing clip: " + clip._name);
	}	
}

var fade = function(clip:MovieClip, out:Boolean, removeAtEnd:Boolean) {
	if (clip.fadeIntervalId)
		stopFade(clip); // replace this fade with a new one

	clip.fadeCount = 0;
	clip.removeAfterFade = removeAtEnd;
	clip.fadeIntervalId = setInterval(function() {			
		var newAlpha:Number = (clip.fadeCount / fadeFrames) * 100.0;
		if (out)
			newAlpha = 100 - newAlpha;
		clip._alpha = newAlpha;
		trace(clip._name +  "._alpha = " + clip._alpha + " count = " + clip.fadeCount);
		if (clip.fadeCount == fadeFrames) {
			stopFade(clip);
		} else {
			clip.fadeCount = clip.fadeCount + 1;
		}	 
	}, fadeTime / fadeFrames);
	trace("started fade interval " + clip.fadeIntervalId + " on movie " + clip._name);
}

var fadeIn = function(clip:MovieClip) {
	fade(clip, false, false);
}

var fadeOut = function(clip:MovieClip, removeAtEnd:Boolean) {
	fade(clip, true, removeAtEnd);
}

var crossFade = function(oldClip:MovieClip, newClip:MovieClip, removeAtEnd:Boolean) {
	if (oldClip != null) {
		fadeOut(oldClip, removeAtEnd);
		trace("fading out old clip " + oldClip);
	}
	if (newClip != null) {
		fadeIn(newClip);
		trace("fading in new clip " + newClip);
	}
}

var clipNum:Number = 0;
var createNewMovie = function() {
	// we use clipNum for the depth, which changes each time; otherwise 
	// the new movie would replace a previous movie at the old depth
	var clip:MovieClip = rootMovie.createEmptyMovieClip("holder" + clipNum, clipNum);
	clipNum = clipNum + 1;
	clip._visible = false;
	
	clip.createEmptyMovieClip("songClips", 1);
	clip.createEmptyMovieClip("themeClips", 0);
	
	clip.songClips.createEmptyMovieClip("albumArtInstance", 0);
	clip.songClips.createTextField("online", 1, 20, 20, 30, 30);
	clip.songClips.createTextField("artist", 2, 40, 40, 30, 30);
	clip.songClips.createTextField("title", 3, 60, 60, 30, 30);

	clip.themeClips.createEmptyMovieClip("backgroundActiveInstance", 1);
	clip.themeClips.createEmptyMovieClip("backgroundInactiveInstance", 0);
	
	clip.defaultImages = { albumArt : baseUrl + "/images/no_image_available75x75light.gif",
							activeBackground : null,
							inactiveBackground : null };
	clip.imageTargets = { albumArt : clip.songClips.albumArtInstance,
							activeBackground : clip.themeClips.backgroundActiveInstance,
							inactiveBackground : clip.themeClips.backgroundInactiveInstance };
	clip.previousImages = {};
	
	clip.pendingLoadItems = 0;
	
	return clip;
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

var setStillPlaying = function(clip:MovieClip, stillPlaying:Boolean) {
	clip.themeClips.backgroundInactiveInstance._visible = !stillPlaying;
	clip.themeClips.backgroundActiveInstance._visible = stillPlaying;
	if (stillPlaying) {
		clip.songClips.online.text = "";
	} else {
		clip.songClips.online.text = "Music stopped";
	}
}

var setSong = function(clip:MovieClip, song:Object) {
	if (songEquals(clip.song, song)) {
		trace("song unchanged, not doing anything");
		return;
	}
	
	clip.song = song;
	clip.songClips.title.text = song.title;
	clip.songClips.artist.text = song.artist;
	setStillPlaying(clip, song.stillPlaying);
	loadImage(clip, "albumArt", song.albumArtUrl);
}

var setTheme = function(clip:MovieClip, theme:Object) {
	if (themeEquals(clip.theme, theme)) {
		trace("theme unchanged, not doing anything");
		return;
	}
	
	clip.theme = theme;
	if (theme.activeImageUrl) 
		loadImage(clip, 'activeBackground', baseUrl + theme.activeImageUrl);
	if (theme.inactiveImageUrl)
		loadImage(clip, 'inactiveBackground', baseUrl + theme.inactiveImageUrl);	
}

var checkSwap = function() {
	if (loadingMovie == null)
		return;
	if (loadingMovie.pendingLoadItems > 0) {
		trace(loadingMovie.pendingLoadItems + " still pending");
		return;
	}
	trace(loadingMovie.pendingLoadItems + " pending items, swapping in");
	trace("currentMovie = " + currentMovie + " loadingMovie = " + loadingMovie);
	var toRemove:MovieClip = currentMovie;
	currentMovie = loadingMovie;
	loadingMovie = null;
	currentMovie._alpha = 0;
	currentMovie._visible = true;
	crossFade(toRemove, currentMovie, true);
}

var songUpdateCount:Number = 0;

var updateSong = function() {
	
	songUpdateCount = songUpdateCount + 1;
	
	if (songUpdateCount > 2) // FIXME a larger number
		return;
	
	if (loadingMovie != null) {
		removeMovie(loadingMovie);
	}
	var clip:MovieClip = createNewMovie();
	loadingMovie = clip;
	
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
			if (node.nodeName == "song") {
				var song = parseSong(node);
				setSong(clip, song);
			} else if (node.nodeName == "theme") {
				var theme = parseTheme(node);
				setTheme(clip, theme);
			}
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
