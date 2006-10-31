dojo.provide("dh.nowplaying");

dojo.require("dh.util");
dojo.require("dh.server");

dh.nowplaying.createNewTheme = function(basedOn) {
	dh.server.getTextPOST("createnewnowplayingtheme",
				     basedOn ? { "basedOn" : basedOn } : {},
		  	    	 function(type, data, http) {	  
		  	    	 	 document.location.href = "/radar-theme-creator?theme=" + data;
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't create the theme");
		  	    	 });
}

dh.nowplaying.setTheme = function(themeId) {
	dh.server.doPOST("setnowplayingtheme",
				     { "theme" : themeId },
		  	    	 function(type, data, http) {	  
		  	    	 	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't set the theme");
		  	    	 });
}

dh.nowplaying.reloadAllEmbeds = function(themeId) {
	var objects = document.getElementsByTagName("object");
	for (var i = 0; i < objects.length; ++i) {
		var object = objects[i];
		var params = object.getElementsByTagName("param");
		for (var j = 0; j < params.length; ++j) {
			var param = params[j];
			if (param.name == "movie" && param.value.indexOf("nowPlaying.swf") > 0 &&
				param.value.indexOf(themeId) > 0) {
				// take object node out and put it back to force a reload
				var parent = object.parentNode;
				var next = object.nextSibling;
				parent.removeChild(object);
				parent.insertBefore(object, next);
				break;
			}
		}
	}
}

dh.nowplaying.modify = function(themeId, key, value, newPage) {
	dh.server.doPOST("modifynowplayingtheme",
				     { "theme" : themeId, "key" : key, "value" : value },
		  	    	 function(type, data, http) {
		  	    	 	if (newPage)
		  	    	 		document.location.href = newPage;
		  	    	 	else
							dh.nowplaying.reloadAllEmbeds(themeId);
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't change the theme");
		  	    	 });
}

dh.nowplaying.applyValue = function(themeId, inputId, valueName) {
	var node = document.getElementById(inputId);
	dh.nowplaying.modify(themeId, valueName, node.value);
}

dh.nowplaying.showChangePhoto = function(n) {
	dh.util.hideId("dhNowPlayingChangePhotoLink" + n)
	dh.util.showId("dhNowPlayingPhotoFileEntry" + n)
	// We set the image here to start it loading so that we hopefully
	// have it already loaded by the time we need to display progress
	var progress = document.getElementById("dhNowPlayingPhotoProgress" + n)
	if (Math.random() < 0.5)
		progress.src = dhImageRoot + "HulaHippo.gif"
	else
		progress.src = dhImageRoot + "HulaPhotographer.gif"
}

dh.nowplaying.doChangePhoto = function(n) {
	dh.util.hideId("dhNowPlayingPhotoFileEntry" + n)
	document.forms["dhNowPlayingPhotoForm" + n].submit()
	var progress = document.getElementById("dhNowPlayingPhotoProgress" + n)
	progress.src = progress.src // Restarts the animation after post in IE
	dh.util.show(progress)
	document.getElementById("dhNowPlayingChangePhotoLink" + n).disabled = true
}
