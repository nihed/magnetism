#include "hippo.as"

var updateCount:Number = 0;

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
	};
	var reqUrl = makeAbsoluteUrl("/xml/usersummary?who=" + who + "&participantOnly=true&includeStack=true");
	meuXML.load(reqUrl);
};

updateSummaryData();
