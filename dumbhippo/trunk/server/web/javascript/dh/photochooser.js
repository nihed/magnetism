dojo.provide("dh.photochooser");
dojo.require("dh.util");
dojo.require("dojo.style");

// FIXME automate getting this from the app server
dh.photochooser.nophoto = '/user_pix1/nophoto.gif';
dh.photochooser.user_pix1 = [ 'alien.gif', 'apricot.gif', 'bear.gif',
							'bear2.gif', 'clown.gif', 'devil.gif', 'duck.gif',
							'eye.gif', 'giraffe.gif', 'greenie.gif', 'kitty.gif',
							'mask.gif', 'monster.gif', 'mysteryman.gif', 'oooh.gif',
							'puffer.gif', 'pug.gif', 'purplehead.gif', 'robo.gif',
							'skull.gif', 'sun.gif', 'tiger.gif', 'tiki.gif', 
							'wolfman.gif' ];

dh.photochooser.pages = [];
dh.photochooser.onSelected = null;
dh.photochooser.showing = false;

dh.photochooser.makePage = function(prefix, photoPool) {
	var page = [];
	// every page starts with "no photo" option
	page.push(dh.photochooser.nophoto);
	// then draw from the pool of images
	while (photoPool.length > 0 && page.length < 16) {
		page.push(prefix + "/" + photoPool.shift())
	}
	return page;
}

dh.photochooser.setPage = function(number) {
	var page = dh.photochooser.pages[number];
	for (var i = 0; i < 16; ++i) {
		var imgNode = document.getElementById('dhPhotoChooserImage' + (i+1));
		var imgLink = imgNode.parentNode;
		
		if (i < page.length) {
			imgNode.src = "/images2" + page[i];
			imgLink.chosenImageName = page[i];
			imgLink.onclick = function() {
				dh.photochooser.onSelected(this.chosenImageName);
			}
			imgLink.style.visibility = 'visible';
		} else {
			imgLink.style.visibility = 'hidden';
			imgLink.onclick = null;
		}
	}
	var hideBack = (number == 0);
	var hideMore = ((number + 1) == dh.photochooser.pages.length);
	var controlsNode = document.getElementById('dhPhotoChooserControls');
	if (hideBack || hideMore)
		controlsNode.style.display = 'block';
	else
		controlsNode.style.display = 'none';
	
	var backNode = document.getElementById('dhPhotoChooserBack');
	if (hideBack)
		backNode.style.display = 'none';
	else
		backNode.style.display = 'block';
	
	var moreNode = document.getElementById('dhPhotoChooserMore');
	if (hideMore)
		moreNode.style.display = 'none';
	else
		moreNode.style.display = 'block';
				
	backNode.onclick = function() {
		dh.photochooser.setPage(number - 1);
	}
	
	moreNode.onclick = function() {
		dh.photochooser.setPage(number + 1);
	}
}

dh.photochooser.reloadPhoto = function(imgAncestorNode, userId, size) {
	var imgNode = null;
	if (imgAncestorNode.nodeName.toLowerCase() == 'img') {
		imgNode = imgAncestorNode;
	} else {
		var imgs = imgAncestorNode.getElementsByTagName('img');
		if (imgs.length != 1) {
			alert("ambiguous reloadPhoto, " + imgs.length + " img tags found");
			return;
		}
		imgNode = imgs[0];
	}
	dh.server.getTextGET("userphoto",
						{ "userId" : userId, 
							"size" : size },
						function(type, data, http) {
							imgNode.src = data;
						},
						function(type, error, http) {
							// displays broken image, better than 
							// appearing to do nothing? maybe
							imgNode.src = null;
						});
}

dh.photochooser.show = function(aboveNode, postSelectFunc) {

	if (dh.photochooser.showing)
		return;
		
	dh.photochooser.onSelected = function(chosenImageName) {
	   	dh.server.doPOST("setstockphoto",
				     		{ "photo" : chosenImageName },
			  	    		function(type, data, http) {
			  	    			//alert("set photo to " + chosenImageName);
								dh.photochooser.hide();
								if (postSelectFunc)
									postSelectFunc.call(this, chosenImageName);
				  	    	},
				  	    	function(type, error, http) {
								alert("Failed to set photo");
				  	    	});
	}
	
	dh.photochooser.setPage(0);
	
	// we assume that "aboveNode" is positioned, or at least 
	// that we want to be relative to its positioned parent
	
	var chooser = document.getElementById('dhPhotoChooser');

	// reparent so the chooser is relative to first possible
	// parent
	var e = aboveNode;
	while (e.nodeName.toUpperCase() != 'DIV') {
		e = e.parentNode;
	}
	
	if (chooser.parentNode != e) {
		chooser.parentNode.removeChild(chooser);
		e.appendChild(chooser);
	}
	
	/* This isn't precision, just "kind of next to" */
	chooser.style.left = "75%";
	chooser.style.bottom = "-20px";
	chooser.style.display = 'block';
	
	document.body.onkeydown = function(ev) {
		if (dh.util.getKeyCode(ev) == ESC) {
			dh.photochooser.hide();
			dh.util.cancelEvent(ev);
			return false;
		}
	}
	
	document.body.onmousedown = function(ev) {
		var target = dh.util.getEventNode(ev);
		if (!target) {
			alert("No event node?");
			return;
		}
		var e = target;
		while (e && e != chooser) {
			e = e.parentNode;
		}
		if (!e) {
			// we weren't a child of the chooser
			dh.photochooser.hide();
		}
		// don't activate something else
		dh.util.cancelEvent(ev);
		return false;
	}
	
	dh.photochooser.showing = true;
}

dh.photochooser.hide = function() {
	if (!dh.photochooser.showing)
		return;

	var chooser = document.getElementById('dhPhotoChooser');
	chooser.style.display = 'none';
	document.body.onmousedown = null;
	document.body.onkeydown = null;
	dh.photochooser.showing = false;
}

dhPhotoChooserInit = function() {
	// slurp into 16-cell pages
	while (dh.photochooser.user_pix1.length > 0) {
		dh.photochooser.pages.push(dh.photochooser.makePage("/user_pix1",
			dh.photochooser.user_pix1));
	}
}

dojo.event.connect(dojo, "loaded", dj_global, "dhPhotoChooserInit");
