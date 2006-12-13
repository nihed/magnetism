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

dh.artist.openSongPlays = function(order, songOrder) {
	document.getElementById("dhSongNamePlaysClosed" + order + "s" + songOrder).style.display = "none"
	document.getElementById("dhSongNamePlaysOpened" + order + "s" + songOrder).style.display = "inline"	
	document.getElementById("dhSongPlaysOpened" + order + "s" + songOrder).style.display = "block"
}

dh.artist.closeSongPlays = function(order, songOrder) {
	document.getElementById("dhSongNamePlaysClosed" + order + "s" + songOrder).style.display = "inline"
	document.getElementById("dhSongNamePlaysOpened" + order + "s" + songOrder).style.display = "none"	
	document.getElementById("dhSongPlaysOpened" + order + "s" + songOrder).style.display = "none"	
}
