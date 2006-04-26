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
dojo.require("dojo.html");

//Do you want to remember what keycode means what? Me neither.
var TAB = 9;
var ESC = 27;
var KEYUP = 38;
var KEYDN = 40;
var ENTER = 13;
var SHIFT = 16;
var CTRL = 17;
var ALT = 18;
var CAPS_LOCK = 20;

/********************************************************
Helper function to determine the keycode pressed in a 
browser-independent manner.
********************************************************/
dh.textinput.getKeyCode = function(ev)
{
	if(ev)			//Moz
	{
		return ev.keyCode;
	}
	if(window.event)	//IE
	{
		return window.event.keyCode;
	}
};

/********************************************************
Helper function to determine the event source element in a 
browser-independent manner.
********************************************************/
dh.textinput.getEventSource = function(ev)
{
	if(ev)			//Moz
	{
		return ev.target;
	}

	if(window.event)	//IE
	{
		return window.event.srcElement;
	}
};

/********************************************************
Helper function to cancel an event in a 
browser-independent manner.
(Returning false helps too).
********************************************************/
dh.textinput.cancelEvent = function(ev)
{
	if(ev)			//Moz
	{
		ev.preventDefault();
		ev.stopPropagation();
	}
	if(window.event)	//IE
	{
		// stop default action of the node
		window.event.returnValue = false;
		// stop bubbling
		window.event.cancelBubble = true;
	}
};

dh.textinput.Entry = function(entryNode, valueIsDefault)
{
	//The 'me' variable allow you to access this object
	//from event handlers
	var me = this;

	//A reference to the input element we're binding to
	this.elem = entryNode;
	
	this.lastValue = null;
	
	this.defaultText = null;
	this.showingDefaultText = false;
	
	// if empty is invalid, we always show default text if value is empty
	this.emptyIsValid = false;
	
	// if it's a password, we only become a password when not in demo/default mode
	this.isPassword = true;	
		
	this._showDefaultText = function() {
		if (!this.showingDefaultText) {
			dojo.html.addClass(this.elem, "dh-entry-showing-default");
			this.elem.value = this.defaultText;
			this.showingDefaultText = true;			
		}
	}
	
	if (valueIsDefault) {
		this.defaultText = this.elem.value;
		this._showDefaultText();
	}
	
	this._hideDefaultText = function() {
		if (this.showingDefaultText) {
			dojo.html.removeClass(this.elem, "dh-entry-showing-default");
			this.elem.value = "";
			this.showingDefaultText = false;
		}
	}

	this._emitValueChanged = function() {
		var v = this.getValue();
		if (v != this.lastValue) {
			this.lastValue = v;
			if (!this.emptyIsValid && (!v || v == "")) {
				this._showDefaultText();
			}
			this.onValueChanged(v);
		}
	}
	
	this.setDefaultText = function(defaultText) {
		this.defaultText = defaultText;
		if (this.showingDefaultText)
			this.elem.value = value;
	}
	
	this.setValue = function(value) {
		this._hideDefaultText();
		this.elem.value = value;
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
	
	this.elem.onchange = function(ev) {
		me._emitValueChanged();
	}
	
	this.elem.onmousedown = function(ev) {
		me._hideDefaultText();
	}
	
	this.elem.onfocusin = function(ev) {
		me._hideDefaultText();
	}
	
	this.elem.onkeydown = function(ev) {
		// in theory never happens since we do this on focus in
		me._hideDefaultText();
		var key = dh.textinput.getKeyCode(ev);
		if (key == ENTER) {
			me._emitValueChanged();
		}
	}
}
