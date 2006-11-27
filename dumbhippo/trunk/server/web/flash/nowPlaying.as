#include "hippo.as"

/////////////////// "model" data types
// (not bothering to make "real" javascript objects, who wants to fight js object system)

var parseColor = function(str:String) {
	if (str.substring(0,1) != "#") {
		trace("hex color doesn't start with #: '" + str + "'");
		return;
	}
	if (str.length != 7) {
		trace("hex string has wrong length: '" + str + "'");
		return;
	}
	return parseInt(str.substring(1,7), 16);
}

var parseTextAttributes = function(node:XMLNode, theme:Object) {
	var what:String = node.attributes.what;
	if (!(what == "album" || what == "artist" || what == "title" || what == "status")) {
		trace("unknown text field " + what);
		return;
	}
	
	if (node.attributes.x)
		theme[what + "X"] = parseInt(node.attributes.x);
	if (node.attributes.y)
		theme[what + "Y"] = parseInt(node.attributes.y);
	if (node.attributes.color)
		theme[what + "Color"] = parseColor(node.attributes.color);
	if (node.attributes.fontSize)
		theme[what + "FontSize"] = parseInt(node.attributes.fontSize);
}

var parseTheme = function(themeNode:XMLNode) {
	//trace(themeNode);
	
	var theme:Object = {};
	
	// Fill in defaults, some of these things aren't even 
	// returned in the xml right now though
	theme.albumArtX = 102;
	theme.albumArtY = 36;
	theme.statusX = 0;
	theme.statusY = 102;
	theme.statusWidth = 203;
	theme.statusHeight = 18;
	theme.statusFontSize = 12;
	theme.statusColor = 0x0000FF;
	theme.artistX = 178;
	theme.artistY = 63;
	theme.artistWidth = 178;
	theme.artistHeight = 22;
	theme.artistFontSize = 14;
	theme.artistColor = 0x0000FF;
	theme.titleX = 176;
	theme.titleY = 41;
	theme.titleWidth = 245;
	theme.titleHeight = 30;
	theme.titleFontSize = 20;
	theme.titleColor = 0x0000FF;
	// offscreen and invisible by default
	theme.albumX = 461;
	theme.albumY = 141;
	theme.albumWidth = 245;
	theme.albumHeight = 30;
	theme.albumFontSize = 0;
	theme.albumColor = 0x0000FF;
	
	var activeImageNodeName;
	var inactiveImageNodeName;
	if (beforeFlash8) {
		activeImageNodeName = "activeImageUrlFlash7";
		inactiveImageNodeName = "inactiveImageUrlFlash7";
	} else {
		activeImageNodeName = "activeImageUrl";
		inactiveImageNodeName = "inactiveImageUrl";		
	}
	
	for (var i = 0; i < themeNode.childNodes.length; ++i) {
		var node:XMLNode = themeNode.childNodes[i];
		if (node.nodeName == activeImageNodeName) {
			theme.activeImageUrl = getNodeContent(node);
		} else if (node.nodeName == inactiveImageNodeName) {
			theme.inactiveImageUrl = getNodeContent(node);
		} else if (node.nodeName == "text") {
			parseTextAttributes(node, theme);
		} else if (node.nodeName == "albumArt") {
			if (node.attributes.x)
				theme.albumArtX = parseInt(node.attributes.x);
			if (node.attributes.y)
				theme.albumArtY = parseInt(node.attributes.y);
		}
	}
	return theme;
}

var parseSong = function(songNode:XMLNode) {
	var song:Object = {};
	for (var i = 0; i < songNode.childNodes.length; ++i) {
		var node:XMLNode = songNode.childNodes[i];
		if (node.nodeName == "title")
			song.title = getNodeContent(node);
		else if (node.nodeName == "artist")
			song.artist = getNodeContent(node);
		else if (node.nodeName == "album")
			song.album = getNodeContent(node);
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
	getURL(baseUrl + "/person?who=" + who, "_self");
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
		//trace(clip._name +  "._alpha = " + clip._alpha + " count = " + clip.fadeCount);
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
	// Disabled for now; because text can't be alpha'd, it just looks 
	// like crap anyway
	
	if (oldClip != null) {
		oldClip._visible = false;
		if (removeAtEnd)
			removeMovie(oldClip);
		//fadeOut(oldClip, removeAtEnd);
		trace("fading out old clip " + oldClip);
	}
	if (newClip != null) {
		newClip._alpha = 100;
		newClip._visible = true;
		//fadeIn(newClip);
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
	
	// happens if no image url was provided at all by the xml
	if (!fullUrl) {
		loadImage(tracker, target, fallbackUrl, null);
		return;
	}
	
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
	songClip.createTextField("status", 1, 0, 0, 0, 0);
	songClip.createTextField("artist", 2, 0, 0, 0, 0);
	songClip.createTextField("title", 3, 0, 0, 0, 0);	
	songClip.createTextField("album", 4, 0, 0, 0, 0);
	
	return songClip;
}

var themeClipNum:Number = 0;
var createNewThemeMovie = function(parent:MovieClip) {
	var themeClip:MovieClip = parent.createEmptyMovieClip("themeClip" + themeClipNum, themeClipNum);
	themeClipNum = themeClipNum + 1;
	themeClip._visible = false;

	themeClip.createEmptyMovieClip("activeBackground", 1);
	themeClip.createEmptyMovieClip("inactiveBackground", 0);
	
	// this will get fixed when we set stillPlaying
	themeClip.activeBackground._visible = false;
	themeClip.inactiveBackground._visible = false;

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
	fontFormat.font = getBestFontName();
	fontFormat.size = fontSize;
	fontFormat.color = color;
	clip.setTextFormat(fontFormat);
}

var themeClipUpdateStillPlaying = function(themeClip:MovieClip, stillPlaying:Boolean) {
	themeClip.inactiveBackground._visible = !stillPlaying;
	themeClip.activeBackground._visible = stillPlaying;
}

var applyTextTheme = function(songClip:MovieClip, theme:Object, what:String) {
	var fontSize = theme[what + "FontSize"];
	
	// trace("font size " + fontSize + " clip " + what);
	
	if (fontSize < 2)
		songClip[what]._visible = false;
	else
		songClip[what]._visible = true;
	
	songClip[what]._x = theme[what + "X"];
	songClip[what]._y = theme[what + "Y"];
	songClip[what]._width = theme[what + "Width"];
	songClip[what]._height = theme[what + "Height"];
	formatText(songClip[what], fontSize, theme[what + "Color"]);
}

var applyThemeToSong = function(songClip:MovieClip, theme:Object) {
	
	trace("applying theme " + theme + " to song " + songClip);
	
	songClip.albumArt._x = theme.albumArtX;
	songClip.albumArt._y = theme.albumArtY;
	
	applyTextTheme(songClip, theme, "status");
	applyTextTheme(songClip, theme, "artist");
	applyTextTheme(songClip, theme, "title");
	applyTextTheme(songClip, theme, "album");
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
		
		// all present and future themes should reflect whether 
		// the song is current playing
		if (clip.loadingThemeClip)
			themeClipUpdateStillPlaying(clip.loadingThemeClip, song.stillPlaying);
		if (clip.currentThemeClip)
			themeClipUpdateStillPlaying(clip.currentThemeClip, song.stillPlaying);

		// the song should reflect the current theme, or the upcoming 
		// theme only if no current theme
		if (clip.currentTheme)
			applyThemeToSong(songClip, clip.currentTheme);
		else if (clip.loadingTheme)
			applyThemeToSong(songClip, clip.loadingTheme);
			
		songClip._alpha = 0;
		songClip._visible = true;
		crossFade(toRemove, songClip, true);
	});
	
	songClip.album.text = song.album;
	songClip.title.text = song.title;
	songClip.artist.text = "by " + song.artist;
	if (song.stillPlaying) {
		songClip.status.text = "";
	} else {
		songClip.status.text = "Music stopped";
	}
	
	// may synchronously invoke the all-loaded callback
	// albumArtUrl may be undefined, in which case we fall back
	trace("setSong() adding album art " + song.albumArtUrl);
	addImageToClip(songClip, songClip.albumArt, song.albumArtUrl,
				   baseUrl + "/images/no_image_available75x75light.gif");
}

var setTheme = function(clip:MovieClip, theme:Object) {
	if (themeEquals(clip.loadingTheme, theme)) {
		trace("theme is already loading theme, not doing anything");
		return;
	}
	if (clip.loadingTheme == null && themeEquals(clip.currentTheme, theme)) {
		trace("theme is already current theme, not doing anything");
		return;
	}
	
	// If there's a current loading theme, we want to replace it
	if (clip.loadingThemeClip != null) {
		removeMovie(clip.loadingThemeClip);
		clip.loadingTheme = null;
		clip.loadingThemeClip = null;
	}
	
	var themeClip:MovieClip = createNewThemeMovie(clip.themeClips);
	clip.loadingThemeClip = themeClip;
	clip.loadingTheme = theme;
	
	setAllImagesLoadedCallback(themeClip, function() {
		if (clip.loadingThemeClip != themeClip) {
			trace("our images all loaded but we aren't the loading theme anymore");
			return;
		}
		// replace current theme clip with ourselves
		trace("swapping in the new theme");
		var toRemove:MovieClip = clip.currentThemeClip;
		clip.currentTheme = clip.loadingTheme;
		clip.currentThemeClip = clip.loadingThemeClip;
		clip.loadingTheme = null;
		clip.loadingThemeClip = null;

		// current theme should reflect whether the current song is playing
		if (clip.currentSong)
			themeClipUpdateStillPlaying(clip.currentThemeClip, clip.currentSong.stillPlaying);
		
		// all present and future song displays should get the now-current theme
		if (clip.currentSongClip)
			applyThemeToSong(clip.currentSongClip, clip.currentTheme);
		if (clip.loadingSongClip)
			applyThemeToSong(clip.loadingSongClip, clip.currentTheme);
		
		themeClip._alpha = 0;
		themeClip._visible = true;
		crossFade(toRemove, themeClip, true);
	});
	
	if (theme.activeImageUrl) {
		trace("theme loading active image");
		addImageToClip(themeClip, themeClip.activeBackground, baseUrl + theme.activeImageUrl, null);
	}
	if (theme.inactiveImageUrl) {
		trace("theme loading inactive image");
		addImageToClip(themeClip, themeClip.inactiveBackground, baseUrl + theme.inactiveImageUrl, null);
	}
}

var updateCount:Number = 0;

var updateNowPlaying = function() {
	
	updateCount = songUpdateCount + 1;
	
	if (updateCount > 1000) // if someone just leaves a browser open, stop eventually
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
setInterval(updateNowPlaying, 1000*60);

// update once on load
updateNowPlaying();
