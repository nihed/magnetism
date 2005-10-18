dojo.provide("dh.sharelink");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.RichText");
if (dojo.version.revision <= 1321) {
	dojo.require("dojo.widget.HtmlButton");
	dojo.require("dojo.xml.*");
	dojo.inherits = dj_inherits;
} else {
	dojo.require("dojo.widget.html.Button");
}
dojo.require("dojo.widget.HtmlComboBox");
dojo.require("dojo.widget.HtmlInlineEditBox");
dojo.require("dojo.widget.Dialog");
dojo.require("dh.server");

// dj_debug("in sharelink.js");

dh.sharelink.submitButtonClicked = function() {
	dj_debug("clicked share link button");
						
	dh.server.getTextPOST("checklogin",
						  {  },
						function(type, data, event) {
							dj_debug("checklogin got back data " + dhAllPropsAsString(data));
							if (true) {
								dojo.debug("showing login dialog");
								//dojo.debug(dhAllPropsAsString(dh.sharelink.loginDialog));
								dh.sharelink.loginDialog.show();
							}
						},
						function(type, error) {
							dj_debug("checklogin got back error " + dhAllPropsAsString(error));
						});

	return;

	dh.server.getXmlGET("friendcompletions",
						{ "entryContents" : "p" },
						function(type, data, event) {
							dj_debug("friendcompletions got back data " + dhAllPropsAsString(data));
						},
						function(type, error) {
							dj_debug("friendcompletions got back error " + dhAllPropsAsString(error));
						});
						
}

dh.sharelink.FriendListProvider = function() {
	this.fakeResults = [
	["Alabama","AL"],
	["Alaska","AK"],
	["American Samoa","AS"],
	["Arizona","AZ"],
	["Arkansas","AR"],
	["Armed Forces Europe","AE"],
	["Armed Forces Pacific","AP"],
	["Armed Forces the Americas","AA"],
	["California","CA"],
	["Colorado","CO"],
	["Connecticut","CT"],
	["Delaware","DE"],
	["District of Columbia","DC"],
	["Federated States of Micronesia","FM"],
	["Florida","FL"],
	["Georgia","GA"],
	["Guam","GU"],
	["Hawaii","HI"],
	["Idaho","ID"],
	["Illinois","IL"],
	["Indiana","IN"],
	["Iowa","IA"],
	["Kansas","KS"],
	["Kentucky","KY"],
	["Louisiana","LA"],
	["Maine","ME"],
	["Marshall Islands","MH"],
	["Maryland","MD"],
	["Massachusetts","MA"],
	["Michigan","MI"],
	["Minnesota","MN"],
	["Mississippi","MS"],
	["Missouri","MO"],
	["Montana","MT"],
	["Nebraska","NE"],
	["Nevada","NV"],
	["New Hampshire","NH"],
	["New Jersey","NJ"],
	["New Mexico","NM"],
	["New York","NY"],
	["North Carolina","NC"],
	["North Dakota","ND"],
	["Northern Mariana Islands","MP"],
	["Ohio","OH"],
	["Oklahoma","OK"],
	["Oregon","OR"],
	["Pennsylvania","PA"],
	["Puerto Rico","PR"],
	["Rhode Island","RI"],
	["South Carolina","SC"],
	["South Dakota","SD"],
	["Tennessee","TN"],
	["Texas","TX"],
	["Utah","UT"],
	["Vermont","VT"],
	["Virgin Islands, U.S.","VI"],
	["Virginia","VA"],
	["Washington","WA"],
	["West Virginia","WV"],
	["Wisconsin","WI"],
	["Wyoming","WY"]
];

	// type is a string "STARTSTRING", "SUBSTRING", "STARTWORD"
	this.startSearch = function(searchStr, type, ignoreLimit) {
		dj_debug("friend startSearch");
		this.provideSearchResults(fakeResults);
	}

	// a "signal", pass it an array of 2-item arrays, where the pairs
	// are ?
	this.provideSearchResults = function(resultsDataPairs) {
		dj_debug("friend provideSearchResults results = " + resultsDataPairs);
	}
}
dojo.inherits(dh.sharelink.FriendListProvider, Object);

dh.sharelink.HtmlFriendComboBox = function(){
	// dj_debug("creating HtmlFriendComboBox");
	dojo.widget.HtmlComboBox.call(this);
	
	this.widgetType = "FriendComboBox";
	this.dataProvider = new dh.sharelink.FriendListProvider();
}

dojo.inherits(dh.sharelink.HtmlFriendComboBox, dojo.widget.HtmlComboBox);

dojo.widget.manager.registerWidgetPackage("dh.sharelink");
dojo.widget.tags.addParseTreeHandler("dojo:friendcombobox");

dh.sharelink.init = function() {
	// dojo.debug("dh.sharelink.init()");
	dh.sharelink.loginDialog = dojo.widget.manager.getWidgetById("dhLoginDialog");
	var btn = document.getElementById("dhLoginDialogButton");
	dh.sharelink.loginDialog.setCloseControl(btn);
	dh.sharelink.loginDialog.setBackgroundColor("#ccc");
	
	// the transparency thing is crazy slow on Linux
	if (dojo.render.html.mozilla && !dojo.render.os.win) {
		dh.sharelink.loginDialog.effect = "";
		dh.sharelink.loginDialog.setBackgroundOpacity(1.0);
	}
}

dhShareLinkInit = dh.sharelink.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareLinkInit");
