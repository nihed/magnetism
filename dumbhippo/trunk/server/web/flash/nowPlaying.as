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

var rootMovie:MovieClip = null;

var removeMovie = function(clip:MovieClip) {
	trace("-------- removing clip: " + clip._name);
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

var createImageTracker = function() {
	var tracker:Object = {};
	tracker.pendingLoadItems = 0;
	return tracker;
}

var setAllLoadedCallback = function(tracker:Object, callback:Function) {
	tracker.allLoadedCallback = callback;
}

var recordOneImageLoadCompleted = function(tracker:Object) {
	tracker.pendingLoadItems = tracker.pendingLoadItems - 1;
	
	if (tracker.pendingLoadItems > 0) {
		trace("pending " + tracker.pendingLoadItems);
		return;
	}
	
	if (tracker.allLoadedCallback) {
		trace("invoking callback since all images loaded");
		tracker.allLoadedCallback();
		tracker.allLoadedCallback = null;
	} else {
		trace("no callback set for completion of image loading");
	}
}

// caution, this can synchronously call the "on all loaded completed" callback
var loadImage = function(tracker:Object, target:MovieClip, fullUrl:String, fallbackUrl:String) {
	trace("loading " + fullUrl);
	var loader:MovieClipLoader = new MovieClipLoader();
	var listener:Object = new Object();
	listener.onLoadError = function(targetClip:MovieClip, errorCode:String) {
		// note, we could be replaced as the loadingMovie already
		trace("failed to load " + fullUrl);
		if (fallbackUrl != null)
			loadImage(tracker, target, fallbackUrl, null); // increments pending image loads
		recordOneImageLoadCompleted(tracker);
	}
	listener.onLoadComplete = function(targetClip:MovieClip) {
		// note, we could be replaced as the loadingMovie already
		trace("completed load of " + fullUrl + " to " + target);
		recordOneImageLoadCompleted(tracker);
	}
	loader.addListener(listener);
	
	tracker.pendingLoadItems = tracker.pendingLoadItems + 1;
	trace("pending " + tracker.pendingLoadItems);
	
	 // returns false if failed to _send_ request
	if (!loader.loadClip(fullUrl, target)) {
		trace("failed to send request for url " + fullUrl);
		recordOneImageLoadCompleted();
		return;
	}
}

var getImageTracker = function(clip:MovieClip) {
	if (!clip.imageTracker) {
		clip.imageTracker = createImageTracker();
	}
	return clip.imageTracker;
}

var addImageToClip = function(clip:MovieClip, target:MovieClip, fullUrl:String, fallbackUrl:String) {
	var tracker:Object = getImageTracker(clip);
	loadImage(tracker, target, fullUrl, fallbackUrl);
}

var setAllImagesLoadedCallback = function(clip:MovieClip, callback:Function) {
	var tracker:Object = getImageTracker(clip);	
	setAllLoadedCallback(tracker, callback);
}

var songClipNum:Number = 0;
var createNewSongMovie = function(parent:MovieClip) {
	var songClip:MovieClip = parent.createEmptyMovieClip("songClip" + songClipNum, songClipNum);
	songClipNum = songClipNum + 1;
	songClip._visible = false;

	songClip.createEmptyMovieClip("albumArt", 0);
	songClip.albumArt._x = 102;
	songClip.albumArt._y = 36;
	songClip.createTextField("online", 1, 0, 102, 203, 18);
	songClip.createTextField("artist", 2, 178, 63, 178, 22);
	songClip.createTextField("title", 3, 176, 41, 245, 30);	
	return songClip;
}

var themeClipNum:Number = 0;
var createNewThemeMovie = function(parent:MovieClip) {
	var themeClip:MovieClip = parent.createEmptyMovieClip("themeClip" + themeClipNum, themeClipNum);
	themeClipNum = themeClipNum + 1;
	themeClip._visible = false;

	themeClip.createEmptyMovieClip("backgroundActiveInstance", 1);
	themeClip.createEmptyMovieClip("backgroundInactiveInstance", 0);
	
	return themeClip;
}

var createRootMovie = function() {
	var clip:MovieClip = createEmptyMovieClip("rootMovie", 0);
	
	// these contain all song clips and all theme clips respectively,
	// in order to keep all the text on top of all the theme background images
	
	clip.createEmptyMovieClip("songClips", 1);
	clip.createEmptyMovieClip("themeClips", 0);
	
	return clip;
}

var formatText = function(clip:MovieClip, fontSize:Number, color:Number) {
	var fontFormat:TextFormat = new TextFormat();
	fontFormat.font = "_sans";
	fontFormat.size = fontSize;
	fontFormat.color = color;
	clip.setTextFormat(fontFormat);
}

var setSong = function(clip:MovieClip, song:Object) {
	if (songEquals(clip.loadingSong, song)) {
		trace("song is already loading song, not doing anything");
		return;
	}
	if (clip.loadingSong == null && songEquals(clip.currentSong, song)) {
		trace("song is already current song, not doing anything");
		return;
	}
	
	// If there's a current loading song, we want to replace it
	if (clip.loadingSongClip != null) {
		removeMovie(clip.loadingSongClip);
		clip.loadingSong = null;
		clip.loadingSongClip = null;
	}
	
	var songClip:MovieClip = createNewSongMovie(clip.songClips);
	clip.loadingSongClip = songClip;
	clip.loadingSong = song;
	
	setAllImagesLoadedCallback(songClip, function() {
		if (clip.loadingSongClip != songClip) {
			trace("our images all loaded but we aren't the loading song anymore");
			return;
		}
		// replace current song clip with ourselves
		trace("swapping in the new song");
		var toRemove:MovieClip = clip.currentSongClip;
		clip.currentSong = clip.loadingSong;
		clip.currentSongClip = clip.loadingSongClip;
		clip.loadingSong = null;
		clip.loadingSongClip = null;
		songClip._alpha = 0;
		songClip._visible = true;
		crossFade(toRemove, songClip, true);
		
		// flip to the right background if needed
		if (false) {
			// FIXME
			clip.themeClips.backgroundInactiveInstance._visible = !song.stillPlaying;
			clip.themeClips.backgroundActiveInstance._visible = song.stillPlaying;
		}
	});
	
	songClip.title.text = song.title;
	songClip.artist.text = "by " + song.artist;
	if (song.stillPlaying) {
		songClip.online.text = "";
	} else {
		songClip.online.text = "Music stopped";
	}
	
	formatText(songClip.online, 12, 0x0000FF);
	formatText(songClip.artist, 14, 0x0000FF);
	formatText(songClip.title, 20, 0x0000FF);
	
	addImageToClip(songClip, songClip.albumArt, song.albumArtUrl,
				   baseUrl + "/images/no_image_available75x75light.gif");
}

var setTheme = function(clip:MovieClip, theme:Object) {

	if (false) {
		if (theme.activeImageUrl) 
			loadImage(clip, 'activeBackground', baseUrl + theme.activeImageUrl);
		if (theme.inactiveImageUrl)
			loadImage(clip, 'inactiveBackground', baseUrl + theme.inactiveImageUrl);	
	}
}

var songUpdateCount:Number = 0;

var updateSong = function() {
	
	songUpdateCount = songUpdateCount + 1;
	
	if (songUpdateCount > 2) // FIXME a larger number
		return;
	
	var meuXML:XML = new XML();
	meuXML.ignoreWhite = true;
	meuXML.onLoad = function(success:Boolean) {
		if (!success) {
			return;
		}
		var root:XML = this.childNodes[0];
		for (var i = 0; i < root.childNodes.length; ++i) {
			var node:XMLNode = root.childNodes[i];
			if (node.nodeName == "song") {
				var song = parseSong(node);
				setSong(rootMovie, song);
			} else if (node.nodeName == "theme") {
				var theme = parseTheme(node);
				setTheme(rootMovie, theme);
			}
		}
	};
	var reqUrl = baseUrl + "/xml/nowplaying?who=" + who;
	if (theme)
		reqUrl = reqUrl + "&theme=" + theme;
	meuXML.load(reqUrl);
};

rootMovie = createRootMovie();

// once per minute
setInterval(updateSong, 1000*3); // FIXME put back

// update once on load
updateSong();
