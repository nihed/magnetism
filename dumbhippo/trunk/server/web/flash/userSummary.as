#include "hippo.as"

import flash.geom.Matrix;

// this gives us our real window size, instead of giving us a fixed size then scaling.
// We can't really handle arbitrary sizes though, only one of the expected ones.
Stage.scaleMode = "noScale";
Stage.align = "TL"; // top left instead of center

var outerBorderWidth:Number = 2;
var paddingInsideOuterBorder:Number = 5;
var presenceIconSize:Number = 12;
var ribbonIconSize:Number = 16;
var blockHeight:Number = 33;

var entireWidth:Number;
var entireHeight:Number;
var stacklessMode:Boolean;
var headshotSize:Number;

var refreshGlobalVariables = function() {
	entireWidth = Stage.width; // 250;
	entireHeight = Stage.height; // 180;
	
	// allow query params to override the width/height, this is mostly a debugging thing.
	// it lets you just open the .swf in a browser without any html.
	if (badgeWidth)
		entireWidth = badgeWidth;
	if (badgeHeight)
		entireHeight = badgeHeight;
	
	// don't let people pick arbitrary sizes, since then we might have to keep them working...
	// clamp to something we know about. Also, when we dynamically hide and show the 
	// flash object in IE, it seems to give it a zero size initially
	
	if (entireWidth != 250)
		entireWidth = 250;
		
	if (!(entireHeight == 74 || entireHeight == 180 || entireHeight == 255)) {
		if (entireHeight > 255)
			entireHeight = 255;
		else if (entireHeight > 180)
			entireHeight = 180;
		else
			entireHeight = 74;
	}
	
	headshotSize = 30;
	
	// See if we need to go into "mini" mode which has no stack and larger headshot
	stacklessMode = entireHeight < 80;
	if (stacklessMode) {
		// we have a larger headshot in this mode
		headshotSize = 60;
	}
}

var rootMovie:MovieClip = createEmptyMovieClip("rootMovie", 0);
var currentView:MovieClip = null;
var currentSummary:Object = null;
var updateInProgress:Boolean = false;
var updateAgainAfterCurrent:Boolean = false;

var simpleGradientFill = function(mc:MovieClip, x:Number, y:Number, width:Number, height:Number, horizontal:Boolean, colors:Array, ratios:Array) {
	var matrix:Matrix = new Matrix();
	matrix.createGradientBox(width, height, horizontal ? 0 : Math.PI / 2);
	var alphas:Array = [];  // 0-100 alpha
	var autoRatios:Array = []; 	// from 0-0xFF, the point where each color starts fading out (I think)
	for (var i = 0; i < colors.length; ++i) {
		alphas.push(100);
		autoRatios.push(i * (0xFF / colors.length));
	}
	if (!ratios)
		ratios = autoRatios;
	mc.beginGradientFill("linear", colors, alphas, ratios, matrix);
	mc.moveTo(x, y);
	mc.lineTo(x + width, y);
	mc.lineTo(x + width, y + height);
	mc.lineTo(x, y + height);
	mc.lineTo(x, y);
	mc.endFill();
}

var createView = function(summary:Object) {
	var viewRoot:MovieClip = rootMovie.createEmptyMovieClip("viewRoot", rootMovie.getNextHighestDepth());

	// gray border
	viewRoot.beginFill(0x999999);
	viewRoot.moveTo(0, 0);
	viewRoot.lineTo(entireWidth, 0);
	viewRoot.lineTo(entireWidth, entireHeight);
	viewRoot.lineTo(0, entireHeight);
	viewRoot.lineTo(0, 0);
	viewRoot.endFill();
	
	// white border
	viewRoot.beginFill(0xffffff);
	viewRoot.moveTo(1, 1);
	viewRoot.lineTo(entireWidth - 1, 1);
	viewRoot.lineTo(entireWidth - 1, entireHeight - 1);
	viewRoot.lineTo(1, entireHeight - 1);
	viewRoot.lineTo(1, 1);
	viewRoot.endFill();	
	
	// gradient background
	simpleGradientFill(viewRoot, outerBorderWidth, outerBorderWidth,
					   entireWidth - outerBorderWidth*2,
					   Math.min(70, entireHeight - outerBorderWidth * 2),
					   false, [ 0xefeeee, 0xffffff ], [0x0, 0xFF] );
	
	var topStuff:MovieClip = viewRoot.createEmptyMovieClip("topStuff", viewRoot.getNextHighestDepth());
	var ribbonBar:MovieClip = viewRoot.createEmptyMovieClip("ribbonBar", viewRoot.getNextHighestDepth());
	var stack:MovieClip;
	if (!stacklessMode)
		stack = viewRoot.createEmptyMovieClip("stack", viewRoot.getNextHighestDepth());
	
	var leftSide = outerBorderWidth + paddingInsideOuterBorder;
	var topSide = outerBorderWidth + paddingInsideOuterBorder;
	var rightSide = entireWidth - outerBorderWidth - paddingInsideOuterBorder;
	
	topStuff._x = leftSide;
	topStuff._y = topSide;

	if (stacklessMode) {
		ribbonBar._x = leftSide + headshotSize + 5;
		// ribbonBar y set later
	} else {
		ribbonBar._x = leftSide;
		// ribbonBar y set later
		stack._x = leftSide;
		// stack y set later
	}
	
	var photo:MovieClip = topStuff.createEmptyMovieClip("photo", topStuff.getNextHighestDepth());
	photo._x = 0;
	photo._y = 0;
	addImageToClip(viewRoot, photo, summary.photo, null);

	var presenceIcon:MovieClip = topStuff.createEmptyMovieClip("presenceIcon", topStuff.getNextHighestDepth());
	presenceIcon._x = photo._x + headshotSize + 5;
	presenceIcon._y = 0;
	addImageToClip(viewRoot, presenceIcon, summary.onlineIcon, null);
	
	var rightEdgeOfPresenceIcon = presenceIcon._x + presenceIconSize;
	
	// the -5 y position of this seems bogus, but otherwise flash screws up and doesn't top-align.
	// not sure what's going on.
	var name:TextField = topStuff.createTextField("name", topStuff.getNextHighestDepth(),
											      rightEdgeOfPresenceIcon + 5, -5, 200, 14);
	name.autoSize = 'left';
	name.html = true;
	name.htmlText = "<b><a href='" + escapeXML(summary.homeUrl) + "'>" + escapeXML(summary.name) + "'s Mugshot</a></b>";
	formatText(name, 14, 0x000000);
	
	var homeLink:TextField = topStuff.createTextField("homeLink", topStuff.getNextHighestDepth(),
													  rightEdgeOfPresenceIcon + 5, name._y + 14 + 5, 200, 12);
	homeLink.autoSize = 'left';
	homeLink.html = true;
	homeLink.htmlText = "<u><a href='" + escapeXML(summary.homeUrl) + "'>Visit my Mugshot page</a></u>";
	formatText(homeLink, 12, 0x0033ff);
	
	var topStuffBottomSide:Number = topSide + Math.max(14 + 5 + 12, headshotSize);
	
	var stackTop:Number;
	
	if (stacklessMode) {
		ribbonBar._y = topStuffBottomSide - ribbonIconSize;
		stackTop = 0;
	} else {
		ribbonBar._y = topStuffBottomSide + 5;
		stackTop = ribbonBar._y + ribbonIconSize + 5;
		stack._y = stackTop;
	}
	
	var ribbon:MovieClip = ribbonBar.createEmptyMovieClip("ribbon", ribbonBar.getNextHighestDepth());
	
	var nextX:Number = 0;
	for (var i = 0; i < summary.accounts.length && ribbonBar._x + nextX + ribbonIconSize < rightSide ; ++i) {
		var account:Object = summary.accounts[i];
		var accountButton:MovieClip = ribbon.createEmptyMovieClip("account" + i, ribbon.getNextHighestDepth());
		accountButton._x = nextX;
		accountButton._y = 0;
		addImageToClip(viewRoot, accountButton, account.icon, null);
		nextX = nextX + ribbonIconSize + 3;
	}

	// return early if stackless
	if (stacklessMode)
		return clip;
	
	// otherwise build the stack
	
	var maxBlocks:Number = Math.floor((entireHeight - outerBorderWidth - paddingInsideOuterBorder - stackTop) / blockHeight);

	trace("can show max of " + maxBlocks + " blocks");
	
	var blockWidth:Number = entireWidth - outerBorderWidth*2 - paddingInsideOuterBorder*2;	
	
	var nextY:Number = 0;
	for (var i = 0; i < Math.min(summary.stack.length, maxBlocks); ++i) {
		var block:Object = summary.stack[i];
		
		var blockClip:MovieClip = stack.createEmptyMovieClip("block" + i, stack.getNextHighestDepth());
		
		// setting background is how we set the movie clip size; just setting _width/_height doesn't seem to work
		simpleGradientFill(blockClip, 0, 0, blockWidth, blockHeight,
					   false, [ 0xededed, 0xf3f3f3 ], [0x0, 0xFF] );
				
		blockClip._x = 0;
		blockClip._y = nextY;
		nextY = nextY + blockHeight + 5;
		//blockClip._height = blockHeight;
		//blockClip._width = 220;

		var maxTextWidth:Number = blockWidth - 10;
		
		var heading:TextField = blockClip.createTextField("heading", blockClip.getNextHighestDepth(), 5, 0, maxTextWidth, 12);
		heading.autoSize = 'left';
		heading.html = true;
		heading.htmlText = "<b>" + escapeXML(block.heading) + "</b>";
		formatText(heading, 11, 0x000000);
		
		var timeAgoSpace = maxTextWidth - heading._width - 5;
		
		if (timeAgoSpace >= 0) {
			var timeAgo:TextField = blockClip.createTextField("timeAgo", blockClip.getNextHighestDepth(), heading._width + 5, 0,
															  timeAgoSpace, 12);
			timeAgo.autoSize = 'left';
			timeAgo.text = "(" + block.timeAgo + ")";
			formatText(timeAgo, 11, 0xaaaaaa);
			
			// hide it if it doesn't fit
			if (timeAgo._width > timeAgoSpace) {
				timeAgo._visible = false;
			}
		} else {
			// the heading may be too long so we need to truncate it
			heading.autoSize = 'none';
			heading._width = maxTextWidth;
			heading._height = 12;
		}
		
		var link:TextField = blockClip.createTextField("link", blockClip.getNextHighestDepth(), 5, 15, maxTextWidth, 17);
		//link.autoSize = 'left'; // we want it to be clipped
		link.html = true;
		link.htmlText = "<u><b><a href='" + escapeXML(block.link) + "'>" + escapeXML(block.linkText) + "</a></b></u>";
		// Linux seems to screw up the font size, so set a smaller one there
		formatText(link, isLinux() ? 9 : 11, 0x0033ff);
		
		trace("height of link is " + link._height + " and bottom of heading is " + (heading._y + heading._height))
	}
	
	return clip;
}

var setSummaryData = function(summary:Object) {
	if (genericEquals(currentSummary, summary))
		return;
	if (currentView) {
		currentView.removeMovieClip();
		currentView = null;
	}
	currentSummary = null;
	if (summary) {
		currentView = createView(summary);
		currentSummary = summary;
	}
}

var parseExternalAccounts = function(externalAccountsNode:XMLNode) {
	var accounts:Array = [];
	for (var i = 0; i < externalAccountsNode.childNodes.length; ++i) {
		var node:XMLNode = externalAccountsNode.childNodes[i];
		
		if (node.nodeName == "externalAccount") {
			var account:Object = {};
			account.link = makeAbsoluteUrl(node.attributes["link"]);
			account.tooltip = makeAbsoluteUrl(node.attributes["linkText"]);
			account.icon = makeAbsoluteUrl(node.attributes["icon"]);
			if (account.link && account.tooltip && account.icon) {
				accounts.push(account);
			} else {
				trace("account missing needed attrs");
			}
		} else {
			trace("ignoring unknown node " + node.nodeName);
		}
	}
	
	return accounts;
}

var parseBlocks = function(stackNode:XMLNode) {
	var blocks:Array = [];

	for (var i = 0; i < stackNode.childNodes.length; ++i) {
		var node:XMLNode = stackNode.childNodes[i];
		
		if (node.nodeName == "block") {
			var block:Object = {};
			block.timeAgo = node.attributes["timeAgo"];
			block.heading = node.attributes["heading"];
			block.link = makeAbsoluteUrl(node.attributes["link"]);
			block.linkText = node.attributes["linkText"];
			if (block.timeAgo && block.heading && block.link && block.linkText) {
				blocks.push(block);
			} else {
				trace("block missing needed attrs");
			}
		} else {
			trace("ignoring unknown node " + node.nodeName);
		}
	}
	
	return blocks;
}

var updateCount:Number = 0;

var updateSummaryData = function() {	
	updateCount = songUpdateCount + 1;
	
	if (updateCount > 1000) // if someone just leaves a browser open, stop eventually
		return;

	// ensure we only have one outstanding update at a time. would be better
	// to cancel the current one, but too lazy
	if (updateInProgress) {
		updateAgainAfterCurrent = true;
		return;
	}
	
	updateInProgress = true;
		
	refreshGlobalVariables();
		
	var meuXML:XML = new XML();
	meuXML.ignoreWhite = true;
	meuXML.onLoad = function(success:Boolean) {
		if (!updateInProgress)
			throw new Error("an update should have been in progress");
		updateInProgress = false;
		
		if (updateAgainAfterCurrent) {
			trace("abandoning this update, starting over");
			updateAgainAfterCurrent = false;
			updateSummaryData();
			return;
		}
		
		if (!success) {
			trace("xml load failure");
			return;
		}
		var root:XML = this.childNodes[0];
		
		if (root.nodeName != "rsp") {
			trace("root node is not rsp");
			return;
		}
		
		if (root.attributes["stat"] != "ok") {
			trace("request status: " + root.attributes["stat"]);
			return;
		}
		
		if (root.childNodes.length < 1) {
			trace("no child nodes of rsp");
			return;
		}

		var summary:Object = {};
		
		var summaryNode:XMLNode = root.childNodes[0];
		
		if (summaryNode.nodeName != "userSummary") {
			trace("wrong node name " + summaryNode.nodeName);
			return;
		}
		
		if (summaryNode.attributes["who"] != who) {
			trace("wrong user " + summaryNode.attributes["who"]);
			return;
		}
		
		summary.who = who;
		summary.photo = makeAbsoluteUrl(summaryNode.attributes["photo"]);
		summary.photo = addQueryParameter(summary.photo, "size", "" + headshotSize);
		summary.online = summaryNode.attributes["online"] == "true";
		summary.onlineIcon = makeAbsoluteUrl(summaryNode.attributes["onlineIcon"]);
		summary.name = summaryNode.attributes["name"];
		summary.homeUrl = makeAbsoluteUrl(summaryNode.attributes["homeUrl"]);	
		summary.accounts = [];
		summary.stack = [];
		
		for (var i = 0; i < summaryNode.childNodes.length; ++i) {
			var node:XMLNode = summaryNode.childNodes[i];
			if (node.nodeName == "accounts") {
				summary.accounts = parseExternalAccounts(node);
			} else if (node.nodeName == "stack") {
				summary.stack = parseBlocks(node);
			} else {
				trace("ignoring unknown node " + node.nodeName);
			}
		}
		
		trace("summary for " + who + " photo " + summary.photo + " " + summary.accounts.length + " accounts " + summary.stack.length + " blocks");
		
		setSummaryData(summary);
	};
	var reqUrl = makeAbsoluteUrl("/xml/usersummary?who=" + who + "&participantOnly=true&includeStack=" + 
								 (stacklessMode ? "false" : "true"));
	meuXML.load(reqUrl);
};

// Update once on load
updateSummaryData();

// Update again if we get resized somehow
var stageListener:Object = new Object();
stageListener.onResize = function() {
    updateSummaryData();
};
Stage.addListener(stageListener);
