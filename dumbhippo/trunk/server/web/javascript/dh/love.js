dojo.provide("dh.love");
dojo.require("dh.util");
dojo.require("dh.formtable");
dojo.require("dh.textinput");
dojo.require("dh.dom");
dojo.require("dh.event");

dh.love.allEntries = {}

dh.love.Entry = function(baseId, defaultLoveText, currentLoveValue)
{
	dh.love.allEntries[baseId] = this;

	// creates a variable that can be captured in closures below, while "this" can't be
	var me = this;

	me._baseId = baseId;
	this._rootNode = document.getElementById(baseId + 'AllId');	
	this._loveNode = document.getElementById(baseId + 'LoveId');
	this._loveEditNode = document.getElementById(baseId + 'LoveEditId');
	this._indifferentNode = document.getElementById(baseId + 'IndifferentId');
	this._busyNode = document.getElementById(baseId + 'BusyId');
	
	this._loveEntryNode = document.getElementById(baseId + 'LoveEntryId');
	
	this._defaultLoveText = defaultLoveText;

	this._loveEntry = new dh.textinput.Entry(this._loveEntryNode, defaultLoveText, currentLoveValue);

	this._loveValue = document.getElementById(baseId + 'LoveValueId');
	this._specialLoveValue = null;

	this._allNodes = [me._loveNode, me._loveEditNode, me._indifferentNode, me._busyNode];
	
	this._errorText = "";
	
	dh.formtable.initExpanded(me._baseId, true);

    // Traverse root node of this widget, calling function on each node
	this._foreachDhIdNode = function(id, func) {
	  dh.util.foreachChildElements(me._rootNode, 	                                    
								   function(node) {
	                                 if (node.getAttribute("dhId") == id) func(node);
	                               });
	}
	
	this._loveEntryNode.onkeydown = function(ev) {
		var key = dh.event.getKeyCode(ev);
		if (key == ENTER) {
			me._saveClicked("love");
		}
	}

	this._showNode = function(node) {
		if (!node)
			throw new Error("null/undefined node passed to _showNode");
		
		// Sync the "not in edit mode" values with the entries, if we're about to show them
		if (node == me._loveNode && me._specialLoveValue == null) {
			dh.dom.textContent(me._loveValue, me._loveEntry.getValue());			
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
			
		dh.formtable.setExpandedEditing(me._baseId, node == me._loveEditNode);
					
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
				else if (each == me._loveEditNode)
					return "loveEdit";
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
	
	this.setError = function(msg) {
		if (msg) {
			me._foreachDhIdNode("DescriptionError",
			                    function(node) {
			                      node.style.display = "block";
			                    });
			me._foreachDhIdNode("DescriptionErrorText",
			                    function(node) {			                    
			                      node.innerHTML = "";
                                  node.appendChild(document.createTextNode(msg));
                                });
			me._foreachDhIdNode("DescriptionNormal",
			                    function(node) {
			                      node.style.display = "none";
			                    });
			
		} else {
			me._foreachDhIdNode("DescriptionNormal",
			                    function(node) {
			                      node.style.display = "block";
			                    });
			me._foreachDhIdNode("DescriptionError",
			                    function(node) {
			                      node.style.display = "none";
			                    });			                    			
		}
		dh.formtable.setExpandedError(me._baseId, msg);
	}
	
	this.setSpecialLoveValue = function(value) {
	    // this is used when we don't want a long value for the entry to be visible 
	    // when the entry is not being edited
	    dh.dom.textContent(me._loveValue, value);	
	    me._specialLoveValue = value;
	}
	
	this.setLoveValueAlreadySaved = function(value) {
		me._loveEntry.setValue(value, true); // true = don't emit changed
	}
	
	// to be set by api user
	this.onLoveSaved = function(value) {
	
	}
	
	// to be set by api user
	this.onCanceled = function() {
	
	}
	
	this._saveClicked = function(mode) {
		if (mode == 'love') {
			me.onLoveSaved(me._loveEntry.getValue());
		} else {
			throw new Error("unknown mode " + mode);
		}
	}
	
	this._cancelClicked = function() {
		me.onCanceled();
	}
	
	// this updates the current values and what's showing
	this.setError(null);
	this.setMode(this.getMode());
	
	return this;
}

dh.love.setMode = function(baseId, mode) {
	var entry = dh.love.allEntries[baseId];
	entry.setMode(mode)
}

dh.love.saveClicked = function(baseId, mode) {
	var entry = dh.love.allEntries[baseId];
	entry._saveClicked(mode);
}

dh.love.cancelClicked = function(baseId) {
	var entry = dh.love.allEntries[baseId];
	entry._cancelClicked();
}
