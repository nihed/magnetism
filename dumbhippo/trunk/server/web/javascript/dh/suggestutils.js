dojo.provide("dh.suggestutils")
dojo.require("dh.model")
dojo.require("dojo.dom");
dojo.require("dojo.string");

// This file provides glue between the generic autosuggest box in dh.autosuggest
// and the data model for groups/users/contacts/etc. implemented in dh.model

dh.suggestutils._makeHighlightNode = function(word, match, type) {
	var index = word.toLowerCase().indexOf(match.toLowerCase());

	var elem = type || 'li';

	var obj = document.createElement(elem);

	var preText = word.substring(0,index);
	var preSpan = document.createElement("span");
	preSpan.appendChild(document.createTextNode(preText));

	var matchText = word.substring(index, index + match.length);
	var matchStrong = document.createElement("strong");
	matchStrong.appendChild(document.createTextNode(matchText));

	var postText = word.substring(index + match.length,word.length);
	var postSpan = document.createElement("span");
	postSpan.appendChild(document.createTextNode(postText));

	obj.appendChild(preSpan);
	obj.appendChild(matchStrong);
	obj.appendChild(postSpan);

	return obj;
}

dh.suggestutils._findInStringArray = function(strings, func, data) {
	for (var i = 0; i < strings.length; ++i) {
		if (func(strings[i], data))
			return dh.suggestutils._makeHighlightNode(strings[i], data);
	}
	return null;
}

dh.suggestutils._sortEligibleCompare = function(a, b) {
	aText = dojo.dom.textContent(a[0])
	bText = dojo.dom.textContent(b[0])	
	if (aText.localeCompare) // don't trust this to exist...
		return aText.localeCompare(bText);
	else {
		if (aText < bText)
			return -1;
		else if (aText > bText)
			return 1;
		else
			return 0;
	}
}

dh.suggestutils.sortEligible = function(array) {
	array.sort(dh.suggestutils._sortEligibleCompare);
	return array;
}

// Return a list of items to display in the autosuggest dropdown 
// when the user pops it down by clicking on the arrow
// @param allKnownIds dictionary of id => possible completion object 
dh.suggestutils.getMenuRecipients = function(allKnownIds) {
	var results = new Array();

	// in menu mode, we just show everyone according to their name
	for (var id in allKnownIds) {
		var obj = allKnownIds[id];
		var node = document.createElement("li");
		if (obj.isPublic == "false") {
			var visibility = document.createElement("img");
			visibility.src = dhImageRoot3 +"lock_icon.png";
			visibility.style.marginRight = "3px";
			node.appendChild(visibility);
		}
		node.appendChild(document.createTextNode(obj.displayName));
		results.push([node, obj.id]);			
	}
	
	return dh.suggestutils.sortEligible(results);	
}

// Return a list of items to display in the autosuggest dropdown 
// that are possible completions for a given string
// @param allKnownIds dictionary of id => possible completion object 
// @param inputText string to match against
// @param omitRecipients any ids found in this dictionary should be skipped
dh.suggestutils.getMatchingRecipients = function(allKnownIds, inputText, omitRecipients) {

	var results = new Array();

	var matchNameFunc = function (word, match) {
		// Check whole word first
		if (word.toLowerCase().indexOf(match.toLowerCase()) == 0) {
			return true;
		} else {
			// Now split the word up on spaces so we match
			// second half of the word just like the first half
			var splitSuggestion = word.split(' ');
			for (j in splitSuggestion) {
				var sug = splitSuggestion[j].toLowerCase();
				if (sug.indexOf(match.toLowerCase()) == 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	var matchEmailFunc = function (word, match) {
		//Check whole email first
		if (word.toLowerCase().indexOf(match.toLowerCase()) == 0) {
			return true;
		} else {
			// Now split the email up on the @ so we match
			// second half of the word just like the first half
			var splitEmail = word.split('@')[0].split('.');
			for (j in splitEmail) {
				var sug = splitEmail[j].toLowerCase();
				if(sug.indexOf(match.toLowerCase()) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	var searchStr = dojo.string.trim(inputText);

	if (searchStr.length == 0)
		return results; // no eligible when the input is empty

	for (var id in allKnownIds) {
		var obj = allKnownIds[id];
		
		var found = null;
		var matchedNode = null;
		if (matchNameFunc(obj.displayName, searchStr)) {
			found = obj;
			matchedNode = dh.suggestutils._makeHighlightNode(obj.displayName, searchStr);
		} else if (obj.email && matchEmailFunc(obj.email, searchStr)) {
			found = obj;
			matchedNode = dh.suggestutils._makeHighlightNode(obj.email, searchStr);
		} else if (obj.aim && matchNameFunc(obj.aim, searchStr)) {
			found = obj;
			matchedNode = dh.suggestutils._makeHighlightNode(obj.aim, searchStr);
		} else {
			// look in all emails and aims; but checking primary
			// email and aim first was deliberate, even though 
			// we'll check them again here
			if (obj.emails) {
				var n = dh.suggestutils._findInStringArray(obj.emails, matchEmailFunc, searchStr);
				if (n) {
					found = obj;
					matchedNode = n;
				}
			}
			if (!found && obj.aims) {
				var n = dh.suggestutils._findInStringArray(obj.aims, matchNameFunc, searchStr);
				if (n) {
					found = obj;
					matchedNode = n;
				}
			}
		}
		
		if (found && found.isPerson() && !found.hasAccount && !found.email) {
			// we can't share with someone who is only an aim address
			found = null;
			matchedNode = null;
		}
		
		if (found) {
			if (!omitRecipients || !dh.model.findGuid(omitRecipients, found.id)) {
				results.push([matchedNode, found.id]);
			}
		}
	}
	
	return dh.suggestutils.sortEligible(results);
}
