if (!baseUrl) {
	// debug base url and person
	baseUrl = "http://localinstance.mugshot.org:8080";
	if (!who)
	    //who = "rQyvYBqJ9Mk7s1";
		who = "c4a3fc1f528070";
}

var isOldFlash = function() {
	var flashVersion:String = getVersion();
	// the version string is bizarre, e.g. "WIN 8,0,1,0"
	// or "LNX 7,0,25,0"
	var i = flashVersion.indexOf(" ");
	var majorStr = flashVersion.substr(i + 1, flashVersion.indexOf(","));
	var major = parseInt(majorStr, 10);
	if (major < 8) {
		return true;		
	} else {
		return false;
	}
}

// Pick our favorite font; unfortunately doesn't fix 
// the "no text in linux" problem most of the time 
// (see https://bugzilla.redhat.com/bugzilla/show_bug.cgi?id=184028)
var _bestFontName = null;
var getBestFontName = function() {
	if (!_bestFontName) {
		var list = TextField.getFontList();
		var found = {};
		var preferences = ['Arial', 'Bitstream Vera Sans', 'Albany',
						   'Verdana', 'Lucida Sans', 'Luxi Sans', 'Helvetica' ];

		//trace(list);
		
		for (var i = 0; i < list.length; ++i) {
			found[list[i]] = 1;
		}
		for (var i = 0; i < preferences.length; ++i) {
			if (found[preferences[i]]) {
				_bestFontName = 'Arial';
				break;
			}
		}
		if (!_bestFontName) {
			_bestFontName = "_sans";
		}
		//trace("best font = " + _bestFontName);
	}
	return _bestFontName;
}

var beforeFlash8 = isOldFlash();

trace("beforeFlash8 = " + beforeFlash8);
trace("best font = " + getBestFontName());

/*
Haven't figured out how to trace() on Linux so we use this bad boy

var displayMessage = function(str:String) {
		var clip:MovieClip = createEmptyMovieClip("rootMovie", 0);

		clip.beginFill(beforeFlash8 ? 0xff0000 : 0x0000ff, 100);
		clip.lineTo(0, 100);
		clip.lineTo(100, 100);
		clip.lineTo(100, 0);
		clip.lineTo(0, 0);
		
		clip.createTextField("message", 1, 0, 0, 440, 120);
		clip.message.text = str;
		var fontFormat:TextFormat = new TextFormat();
		fontFormat.font = getBestFontName();
		fontFormat.size = 20;
		fontFormat.color = 0x000000;
		fontFormat.align = "center";
		clip.message.setTextFormat(fontFormat);
		clip.message._visible = true;
		clip.visible = true;
}

displayMessage("Flash version is " + getVersion() + " before 8: " + isBeforeFlash8);
return;
*/

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

var makeAbsoluteUrl = function(s:String) {
	if (s.charAt(0) == '/')
		return baseUrl + s;
	else
		return s;
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
	trace("loading to " + target._name + " url " + fullUrl);
	
	// happens if no image url was provided at all by the xml
	if (!fullUrl) {
		loadImage(tracker, target, fallbackUrl, null);
		return;
	}
	
	var loader:MovieClipLoader = new MovieClipLoader();
	var listener:Object = new Object();
	listener.onLoadError = function(targetClip:MovieClip, errorCode:String) {
		// note, we could be replaced as the loadingMovie already
		trace("failed to load " + fullUrl + " to " + target._name);
		if (fallbackUrl != null)
			loadImage(tracker, target, fallbackUrl, null); // increments pending image loads
		recordOneImageLoadCompleted(tracker);
	}
	listener.onLoadComplete = function(targetClip:MovieClip) {
		// note, we could be replaced as the loadingMovie already
		trace("completed load of " + fullUrl + " to " + target._name);
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

var formatText = function(clip:MovieClip, fontSize:Number, color:Number) {
	var fontFormat:TextFormat = new TextFormat();
	fontFormat.font = getBestFontName();
	fontFormat.size = fontSize;
	fontFormat.color = color;
	clip.setTextFormat(fontFormat);
}

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

var escapeXML = function(str:String) {
	// better ideas? go nuts
	var doc:XML = new XML();
	var node:XMLNode = doc.createTextNode(str);	
	//doc.appendChild(node);
	
	return node.toString();
}
