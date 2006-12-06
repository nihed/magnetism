dojo.provide("dh.lovehate");
dojo.require("dh.util");
dojo.require("dh.textinput");
dojo.require("dojo.html");

dh.lovehate.allEntries = {}

dh.lovehate.Entry = function(baseId, defaultLoveText, currentLoveValue, defaultHateText, currentHateValue)
{
	dh.lovehate.allEntries[baseId] = this;

	// creates a variable that can be captured in closures below, while "this" can't be
	var me = this;

	this._rootNode = document.getElementById(baseId + 'AllId');
	this._loveNode = document.getElementById(baseId + 'LoveId');
	this._hateNode = document.getElementById(baseId + 'HateId');
	this._loveEditNode = document.getElementById(baseId + 'LoveEditId');
	this._hateEditNode = document.getElementById(baseId + 'HateEditId');
	this._indifferentNode = document.getElementById(baseId + 'IndifferentId');
	this._busyNode = document.getElementById(baseId + 'BusyId');
	
	this._loveEntryNode = document.getElementById(baseId + 'LoveEntryId');
	this._hateEntryNode = document.getElementById(baseId + 'HateEntryId');
	
	this._defaultLoveText = defaultLoveText;
	this._defaultHateText = defaultHateText;

	this._loveEntry = new dh.textinput.Entry(this._loveEntryNode, defaultLoveText, currentLoveValue);
	this._hateEntry = new dh.textinput.Entry(this._hateEntryNode, defaultHateText, currentHateValue);
	
	this._loveValue = document.getElementById(baseId + 'LoveValueId');
	this._specialLoveValue = null;
	this._hateValue = document.getElementById(baseId + 'HateValueId');
	
	this._allNodes = [me._loveNode, me._hateNode, me._loveEditNode, me._hateEditNode, me._indifferentNode, me._busyNode];

	this._loveEntryNode.onkeydown = function(ev) {
		var key = dh.util.getKeyCode(ev);
		if (key == ENTER) {
			me._saveClicked("love");
		}
	}

	this._hateEntryNode.onkeydown = function(ev) {
		var key = dh.util.getKeyCode(ev);
		if (key == ENTER) {
			me._saveClicked("hate");
		}
	}
	
	this._showNode = function(node) {
		if (!node)
			throw new Error("null/undefined node passed to _showNode");
		
		// Sync the "not in edit mode" values with the entries, if we're about to show them
		if (node == me._loveNode && me._specialLoveValue == null) {
			dojo.dom.textContent(me._loveValue, me._loveEntry.getValue());			
		}
		if (node == me._hateNode) {
			dojo.dom.textContent(me._hateValue, me._hateEntry.getValue());			
		}
		
		var i;
		for (i = 0; i < me._allNodes.length; ++i) {
			var each = me._allNodes[i];
			if (!each)
				throw new Error("null/undefined node in allNodes " + i);
			if (each == node)
				each.style.display = 'block';
			else
				each.style.display = 'none';
		}
		if (node.style.display == 'none')
			throw new Error("node " + node.id + " was not found...");
					
		// focus entry boxes
		// we don't do this because it hides the gray hint text
		//if (node == me._loveEditNode)
		//	me._loveEntryNode.focus();
		//if (node == me._hateEditNode)
		//	me._hateEntryNode.focus();
	}
	
	this.getMode = function() {
		var i;
		for (i = 0; i < me._allNodes.length; ++i) {
			var each = me._allNodes[i];
			if (!each)
				throw new Error("null/undefined node in allNodes " + i);
			if (each.style.display == 'block') {
				if (each == me._loveNode)
					return "love";
				else if (each == me._hateNode)
					return "hate";
				else if (each == me._loveEditNode)
					return "loveEdit";
				else if (each == me._hateEditNode)
					return "hateEdit";
				else if (each == me._indifferentNode)
					return "indifferent";
				else if (each == me._busyNode)
					return "busy";
			}
		}
		return null;
	}
	
	this.getLoveEntry = function() {
	    return me._loveEntry;
	}
	
	this.setMode = function(mode) {
		var nodeVar = "_" + mode + "Node";
		var node = me[nodeVar];
		if (!node)
			throw new Error("invalid mode '" + mode + "' leading to var '" + nodeVar + "'");
		me._showNode(node);	
	}
	
	this.setBusy = function() {
		me.setMode('busy');
	}
	
	this.setSpecialLoveValue = function(value) {
	    // this is used when we don't want a long value for the entry to be visible 
	    // when the entry is not being edited
	    dojo.dom.textContent(me._loveValue, value);	
	    me._specialLoveValue = value;
	}
	
	this.setLoveValueAlreadySaved = function(value) {
		me._loveEntry.setValue(value, true); // true = don't emit changed
	}
	
	// to be set by api user
	this.onLoveSaved = function(value) {
	
	}
	
	// to be set by api user
	this.onHateSaved = function(value) {
	
	}
	
	// to be set by api user
	this.onCanceled = function() {
	
	}
	
	this._saveClicked = function(mode) {
		if (mode == 'love') {
			me.onLoveSaved(me._loveEntry.getValue());
		} else if (mode == 'hate') {
			if (me._hateEntry.getValue() == "") // If you just want to hate, hate on and we'll take care of your message
				me._hateEntry.setValue(this._defaultHateText);

			me.onHateSaved(me._hateEntry.getValue());
		} else {
			throw new Error("unknown mode " + mode);
		}
	}
	
	this._cancelClicked = function() {
		me.onCanceled();
	}
	
	// this updates the current values and what's showing
	this.setMode(this.getMode());
}

dh.lovehate.setMode = function(baseId, mode) {
	var entry = dh.lovehate.allEntries[baseId];
	entry.setMode(mode)
}

dh.lovehate.saveClicked = function(baseId, mode) {
	var entry = dh.lovehate.allEntries[baseId];
	entry._saveClicked(mode);
}

dh.lovehate.cancelClicked = function(baseId) {
	var entry = dh.lovehate.allEntries[baseId];
	entry._cancelClicked();
}
