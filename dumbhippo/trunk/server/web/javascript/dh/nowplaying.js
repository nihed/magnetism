dojo.provide("dh.nowplaying");

dojo.require("dh.util");
dojo.require("dh.server");

dh.nowplaying.createNewTheme = function(basedOn) {
	dh.server.getTextPOST("createnewnowplayingtheme",
				     { "basedOn" : basedOn },
		  	    	 function(type, data, http) {	  
		  	    	 	 document.location.href = "/nowplaying-theme-creator?theme=" + data;
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

dh.nowplaying.modify = function(themeId, key, value) {
	dh.server.doPOST("modifynowplayingtheme",
				     { "theme" : themeId, "key" : key, "value" : value },
		  	    	 function(type, data, http) {	  
		  	    	 	 document.location.reload();
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't change the theme");
		  	    	 });
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
