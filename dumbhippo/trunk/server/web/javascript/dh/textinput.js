/*******************************************************

Based on AutoSuggest - a javascript automatic text input completion component
Copyright (C) 2005 Joe Kepley, The Sling & Rock Design Group, Inc.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

dojo.provide("dh.textinput");
dojo.require("dh.util");
dojo.require("dh.html");

dh.textinput.Entry = function(entryNode, defaultText, currentValue)
{
	//The 'me' variable allow you to access this object
	//from event handlers
	var me = this;

	if (!entryNode)
		throw new Error("null entryNode in dh.textinput.Entry");
	if (entryNode.nodeName.toLowerCase() != "input" && entryNode.nodeName.toLowerCase() != "textarea")
		throw new Error("entryNode in dh.textinput.Entry has node name " + entryNode.nodeName);

	//A reference to the input element we're binding to
	this.elem = entryNode;
	if (currentValue)
		this.elem.value = currentValue;
	else
		this.elem.value = ""; // overwrite anything the browser saved over a reload
	
	this.lastValue = this.elem.value;
	
	this.defaultText = defaultText;
	this.showingDefaultText = false;
	
	// if empty is invalid, we always show default text if value is empty
	this.emptyIsValid = false;
			
	this._showDefaultText = function() {
		if (!this.showingDefaultText && this.defaultText) {
			dh.html.addClass(this.elem, "dh-text-input-showing-default");
			this.elem.value = this.defaultText;
			this.showingDefaultText = true;			
		}
	}
	
	if (this.defaultText && this.elem.value.length == 0) {
		this._showDefaultText();
	}
	
	this._emitValueChanged = function() {
		var v = this.getValue();
		dh.debug("v = '" + v + "'");
		if (!this.emptyIsValid && (!v || v.length == 0)) {
			this._showDefaultText();
		}
		if (v != this.lastValue) {
			this.lastValue = v;
			this.onValueChanged(v);
		}
	}

	// be sure we only have real data in the entry so we can submit a form
	this.prepareToSubmit = function() {
		this.elem.value = this.getValue();
	}
	
	this.hideDefaultText = function() {
		if (this.showingDefaultText) {
			dh.html.removeClass(this.elem, "dh-text-input-showing-default");
			this.elem.value = "";
			this.showingDefaultText = false;
		}
	}
	
	this.setDefaultText = function(defaultText) {
		this.defaultText = defaultText;
		if (this.showingDefaultText)
			this.elem.value = value;
	}
	
	this.setValue = function(value, noEmitChanged) {
		this.hideDefaultText();
		this.elem.value = value;
		if (noEmitChanged)
			this.lastValue = value;
		this._emitValueChanged();
	}
	
	this.getValue = function() {
		if (this.showingDefaultText)
			return "";
		else
			return this.elem.value;
	}
	
	this.onValueChanged = function() {
		
	}
	
	this.setDisabled = function() {
		this.elem.disabled = true;
	}

	this.setEnabled = function() {
		this.elem.disabled = false;
	}
		
	this.activate = function() {
		this._emitValueChanged();
	}
	
	this.elem.onchange = function(ev) {
		me.activate();
	}

	this.elem.onfocus = function(ev) {
		me.hideDefaultText();
	}
	
	this.elem.onblur = function(ev) {
		me.activate();
	}
	
	this.elem.onkeydown = function(ev) {
		// in theory never happens since we do this on focus in
		me.hideDefaultText();
		var key = dh.util.getKeyCode(ev);
		if (key == ENTER && me.elem.nodeName.toUpperCase() != 'TEXTAREA') {
			me.activate();
		}
	}
}
