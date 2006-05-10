dojo.provide("dh.artist");
dojo.require("dh.util");

dh.artist.openSongList = function(order) {
	document.getElementById("dhSongListClosed" + order).style.display = "none"
	document.getElementById("dhSongListOpened" + order).style.display = "block"	
}

dh.artist.closeSongList = function(order) {
	document.getElementById("dhSongListClosed" + order).style.display = "block"
	document.getElementById("dhSongListOpened" + order).style.display = "none"	
}