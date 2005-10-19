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

dh.sharelink.urlToShareEditBox = null;
dh.sharelink.descriptionRichText = null;

dh.sharelink.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	var urlHtml = dh.sharelink.urlToShareEditBox.textValue;
	var descriptionHtml = dh.sharelink.descriptionRichText.getEditorContent();
	
	dojo.debug("url = " + urlHtml);
	dojo.debug("desc = " + descriptionHtml);
	
	// FIXME we don't really want to send HTML to the server... at least not 
	// without "simplification" to tags we understand which would be easy on 
	// client side...
	
	// double-check that we're logged in
	dh.login.requireLogin(function() {					
		dh.server.doPOST("sharelink",
						{ 
							"url" : urlHtml, 
						  	"description" : descriptionHtml,
						  	"recipients" : "" // FIXME
						},
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
		// DEBUG - put data in the default provider
		//this.dataProvider = new dojo.widget.ComboBoxDataProvider();
		//this.dataProvider.setData(dh.sharelink.stateNames);
    }
}

dojo.inherits(dh.sharelink.HtmlFriendComboBox, dojo.widget.HtmlComboBox);

dojo.widget.manager.registerWidgetPackage("dh.sharelink");
dojo.widget.tags.addParseTreeHandler("dojo:friendcombobox");

dh.sharelink.init = function() {
	dojo.debug("dh.sharelink.init");
	
	// all the dojo is set up now, so show the body; this is to reduce flicker
	dojo.html.removeClass(document.body, "dhInvisible");
	
	dh.login.requireLogin(function() {
		dojo.debug("dh.sharelink logged in!");
		var params = dh.util.getParamsFromLocation();
		dh.sharelink.urlToShareEditBox = dojo.widget.manager.getWidgetById("dhUrlToShare");
		if (dojo.lang.has(params, "url")) {
			// FIXME InlineEditBox takes HTML, even though it's called setText, need to escape
			dh.sharelink.urlToShareEditBox.setText(params["url"]);
		}
		dh.sharelink.descriptionRichText = dojo.widget.manager.getWidgetById("dhShareLinkDescription");
	});
}

dhShareLinkInit = dh.sharelink.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareLinkInit");
