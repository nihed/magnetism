dojo.provide("dh.photochooser");
dojo.require("dh.util");
dojo.require("dojo.style");
dojo.require("dh.popup");

// FIXME automate getting this from the app server
dh.photochooser.user_nophoto = '/user_pix1/nophoto.gif';
dh.photochooser.user_pix1 = [ 'alien.gif', 'apricot.gif', 'bear.gif',
			      'bear2.gif', 'clown.gif', 'devil.gif', 'duck.gif',
			      'eye.gif', 'giraffe.gif', 'greenie.gif', 'kitty.gif',
			      'mask.gif', 'monster.gif', 'mysteryman.gif', 'oooh.gif',
			      'puffer.gif', 'pug.gif', 'purplehead.gif', 'robo.gif',
			      'skull.gif', 'sun.gif', 'tiger.gif', 'tiki.gif', 
			      'wolfman.gif', 'ghost.gif', 'hippo.gif', 'monkey.gif',
			      'bloo.gif', 'cactoo.gif', 'eggwin.gif', 'goobli.gif',
			      'jojo.gif', 'mutt.gif', 'burnie.gif', 'drippy.gif', 'flim.gif',
			      'hrvatchki.gif', 'leafy.gif', 'squidley.gif' ];

dh.photochooser.group_nophoto = '/group_pix1/nogroupphoto.gif';
dh.photochooser.group_pix1 = [ 'baseball.gif', 'bingo.gif', 'birds.gif',
							   'bowling.gif', 'cards.gif', 'cows.gif', 'dudes.gif',
							   'fish.gif', 'flock.gif', 'flowers.gif', 'geese.gif',
							   'kittens.gif', 'penguins.gif', 'pills.gif',
							   'pirates.gif' ];

dh.photochooser.type = "user"
dh.photochooser.pages = [];
dh.photochooser.onSelected = null;

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
			// This is a hack to make sure IE loads all the images (bug 576)
			var tmpImg = new Image();
			tmpImg.src = "/images2" + page[i];

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
	if (hideBack && hideMore)
		controlsNode.style.display = 'none';
	else
		controlsNode.style.display = 'block';
	
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

dh.photochooser.reloadPhoto = function(imgAncestorNodes, size) {
	var imgNodes = [];
	for (var i = 0; i < imgAncestorNodes.length; i++) {
		var imgAncestorNode = imgAncestorNodes[i]
		var imgNode
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
		imgNodes.push(imgNode)
	}
	
	var method
	var args = { "size" : size }
	
	if (dh.photochooser.type == "user") {
		method = "userphoto"
		args["userId"] = dh.photochooser.id
	} else {
		method = "groupphoto"
		args["groupId"] = dh.photochooser.id
	}
	
	dh.server.getTextGET(method, args,
						function(type, data, http) {
							for (var i = 0; i < imgNodes.length; i++) {
								imgNodes[i].src = data;
							}
						},
						function(type, error, http) {
							// displays broken image, better than 
							// appearing to do nothing? maybe
							imgNode.src = null;
						});
}

dh.photochooser.show = function(aboveNode, postSelectFunc) {
		
	if (dh.popup.isShowing('dhPhotoChooser'))
		return;
		
	dh.photochooser.onSelected = function(chosenImageName) {
		var method
		var args = { "photo" : chosenImageName }
		if (dh.photochooser.type == "user") {
			method = "setstockphoto"
		} else {
			method = "setgroupstockphoto"
			args["groupId"] = dh.photochooser.id
		}
			
	   	dh.server.doPOST(method, args,
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
	
	dh.popup.show('dhPhotoChooser', aboveNode);
}

dh.photochooser.hide = function() {
	dh.popup.hide('dhPhotoChooser');
}

dh.photochooser.init = function(type, id) {
	if (type == null)
		type = "user"

	dh.photochooser.type = type
	dh.photochooser.id = id

	var url
	var pix
	if (type == "user") {	
		url = "/user_pix1" 
		pix = dh.photochooser.user_pix1
		dh.photochooser.nophoto = dh.photochooser.user_nophoto
	} else { 
		url = "/group_pix1" 
		pix = dh.photochooser.group_pix1
		dh.photochooser.nophoto = dh.photochooser.group_nophoto
	}

	// slurp into 16-cell pages
	while (pix.length > 0) {
		dh.photochooser.pages.push(dh.photochooser.makePage(url, pix))
	}
}
