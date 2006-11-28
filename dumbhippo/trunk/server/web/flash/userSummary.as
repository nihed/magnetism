#include "hippo.as"

import flash.geom.Matrix;

var rootMovie:MovieClip = createEmptyMovieClip("rootMovie", 0);
var currentView:MovieClip = null;
var currentSummary:Object = null;

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

	simpleGradientFill(viewRoot, 0, 0, 260, 220, false, [ 0xcccccc, 0xfefefe, 0xffffff ], [0x0, 0x20, 0xFF] );

	var topStuff:MovieClip = viewRoot.createEmptyMovieClip("topStuff", viewRoot.getNextHighestDepth());
	var ribbonBar:MovieClip = viewRoot.createEmptyMovieClip("ribbonBar", viewRoot.getNextHighestDepth());
	var stack:MovieClip = viewRoot.createEmptyMovieClip("stack", viewRoot.getNextHighestDepth());
	
	var photo:MovieClip = topStuff.createEmptyMovieClip("photo", topStuff.getNextHighestDepth());
	photo._x = 10;
	photo._y = 5;
	addImageToClip(viewRoot, photo, summary.photo, null);

	var presenceIcon:MovieClip = topStuff.createEmptyMovieClip("presenceIcon", topStuff.getNextHighestDepth());
	presenceIcon._x = 80;
	presenceIcon._y = 5;
	addImageToClip(viewRoot, presenceIcon, summary.onlineIcon, null);
	
	var name:TextField = topStuff.createTextField("title", topStuff.getNextHighestDepth(), 94, 5, 200, 50);
	name.autoSize = 'left';
	name.html = true;	
	name.htmlText = "<b><a href='" + escapeXML(summary.homeUrl) + "'>" + escapeXML(summary.name) + "'s Mugshot</a></b>";
	formatText(name, 14, 0x000000);
	
	var homeLink:TextField = topStuff.createTextField("visitLink", topStuff.getNextHighestDepth(), 94, 20, 200, 50);
	homeLink.autoSize = 'left';
	homeLink.html = true;
	homeLink.htmlText = "<a href='" + escapeXML(summary.homeUrl) + "'>Visit my Mugshot page</a>";
	formatText(homeLink, 12, 0x0000ff);
	
	var ribbon:MovieClip = ribbonBar.createEmptyMovieClip("ribbon", ribbonBar.getNextHighestDepth());
	ribbon._x = 10;
	ribbon._y = 72;
	
	var nextX:Number = 0;
	for (var i = 0; i < summary.accounts.length; ++i) {
		var account:Object = summary.accounts[i];
		var accountButton:MovieClip = ribbon.createEmptyMovieClip("account" + i, ribbon.getNextHighestDepth());
		accountButton._x = nextX;
		accountButton._y = 0;
		addImageToClip(viewRoot, accountButton, account.icon, null);
		nextX = nextX + 18;
	}
	
	var blockHeight:Number = 35;
	var nextY:Number = 92;
	for (var i = 0; i < summary.stack.length; ++i) {
		var block:Object = summary.stack[i];
		
		var blockClip:MovieClip = stack.createEmptyMovieClip("block" + i, stack.getNextHighestDepth());
		
		// this is how we set the movie clip size; just setting _width/_height doesn't seem to work
		blockClip.beginFill(0xeeeeee);
		blockClip.moveTo(0, 0);
		blockClip.lineTo(240, 0);
		blockClip.lineTo(240, blockHeight);
		blockClip.lineTo(0, blockHeight);
		blockClip.lineTo(0, 0);
		blockClip.endFill();
		
		blockClip._x = 10;
		blockClip._y = nextY;
		//blockClip._height = blockHeight;
		//blockClip._width = 220;

		var heading:TextField = blockClip.createTextField("heading", blockClip.getNextHighestDepth(), 5, 0, 200, 20);
		heading.autoSize = 'left';
		heading.html = true;
		heading.htmlText = "<b>" + escapeXML(block.heading) + "</b>";
		formatText(heading, 12, 0x000000);
		
		var timeAgo:TextField = blockClip.createTextField("timeAgo", blockClip.getNextHighestDepth(), heading._width + 5, 0, 200, 20);
		timeAgo.autoSize = 'left';
		timeAgo.text = block.timeAgo;
		formatText(timeAgo, 12, 0xaaaaaa);
		
		var link:TextField = blockClip.createTextField("link", blockClip.getNextHighestDepth(), 5, 15, 200, 20);
		link.autoSize = 'left';
		link.html = true;
		link.htmlText = "<u><b><a href='" + escapeXML(block.link) + "'>" + escapeXML(block.linkText) + "</a></b></u>";
		formatText(link, 12, 0x0000ff);	
		
		nextY = nextY + blockHeight + 5;
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
	
	var meuXML:XML = new XML();
	meuXML.ignoreWhite = true;
	meuXML.onLoad = function(success:Boolean) {
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
		summary.photo = makeAbsoluteUrl(summaryNode.attributes["photo"] + "?size=60");
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
	var reqUrl = makeAbsoluteUrl("/xml/usersummary?who=" + who + "&participantOnly=true&includeStack=true");
	meuXML.load(reqUrl);
};

updateSummaryData();
