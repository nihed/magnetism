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
dojo.require("dh.server");
dojo.require("dh.login");
dojo.require("dh.util");
dojo.require("dh.model");

// hash of all persons we have ever loaded up, keyed by personId
dh.sharelink.allKnownPersons = {}
// currently selected recipients
dh.sharelink.selectedRecipients = []

dh.sharelink.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	// double-check that we're logged in
	dh.login.requireLogin(function() {					
		dh.server.doPOST("sharelink",
						{ },
						function(type, data, http) {
							dojo.debug("sharelink got back data " + dhAllPropsAsString(data));
						},
						function(type, error, http) {
							dojo.debug("sharelink got back error " + dhAllPropsAsString(error));
						});
	});
}

dh.sharelink.stateNames = [
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

dh.sharelink.stateGetResults = function(searchStr, type, ignoreLimit) {
	var copy = [];
	for (var i = 0; i < dh.sharelink.stateNames.length; ++i) {
		if (dh.sharelink.stateNames[i][0].length >= searchStr.length &&
			dh.sharelink.stateNames[i][0].substring(0, searchStr.length).toLowerCase() == searchStr) {
			var subcopy = [];
			for (var j = 0; j < 2; ++j) {
				subcopy.push(dh.sharelink.stateNames[i][j]);
			}
			copy.push(subcopy);
		}
	}
	return copy;
}

dh.sharelink.FriendListProvider = function() {

	// type is a string "STARTSTRING", "SUBSTRING", "STARTWORD"
	this.startSearch = function(searchStr, type, ignoreLimit) {
		//dojo.debug("friend startSearch");
		var _this = this;
		
		dh.server.getXmlGET("friendcompletions",
							{ "entryContents" : searchStr },
							function(type, data, http) {
								dojo.debug("friendcompletions got back data " + data);
								//dojo.debug("text is : " + http.responseText);
								dojo.debug(data.doctype);
								
								var completions = [];
								
								var personNodeList = data.getElementsByTagName("person");
								for (var i = 0; i < personNodeList.length; ++i) {
									var pelement = personNodeList.item(i);
									var person = dh.model.personFromXmlNode(pelement);
									var completion = pelement.getAttribute("completion");
									
									completions.push([completion, person.id]);
									
									// merge in a new person we know about
									dh.sharelink.allKnownPersons[person.id] = person;
								}
								
								_this.provideSearchResults(completions);
							},
							function(type, error, http) {
								dojo.debug("friendcompletions got back error " + dhAllPropsAsString(error));
							});
	}

	// a "signal", pass it an array of 2-item arrays, where the pairs
	// are ???? ; BEWARE dojo destroys this array so pass it a copy
	// if you are also keeping a reference
	this.provideSearchResults = function(resultsDataPairs) {
		//dojo.debug("friend provideSearchResults results = " + resultsDataPairs);
	}
}
dojo.inherits(dh.sharelink.FriendListProvider, Object);

dh.sharelink.HtmlFriendComboBox = function(){
	// dojo.debug("creating HtmlFriendComboBox");
	dojo.widget.HtmlComboBox.call(this);
	
	this.widgetType = "FriendComboBox";
	
	this.fillInTemplate = function(args, frag){
		// override the default provider
		this.dataProvider = new dh.sharelink.FriendListProvider();
    }
}

dojo.inherits(dh.sharelink.HtmlFriendComboBox, dojo.widget.HtmlComboBox);

dojo.widget.manager.registerWidgetPackage("dh.sharelink");
dojo.widget.tags.addParseTreeHandler("dojo:friendcombobox");

dh.sharelink.urlToShareEditBox = null;

dh.sharelink.init = function() {
	dojo.debug("dh.sharelink.init");
	
	dh.login.requireLogin(function() {
		dojo.debug("dh.sharelink logged in!");
		var params = dh.util.getParamsFromLocation();
		dh.sharelink.urlToShareEditBox = dojo.widget.manager.getWidgetById("dhUrlToShare");
		if (dojo.lang.has(params, "url")) {
			// FIXME InlineEditBox takes HTML, even though it's called setText, need to escape
			dh.sharelink.urlToShareEditBox.setText(params["url"]);
		}
	});
}

dhShareLinkInit = dh.sharelink.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareLinkInit");
