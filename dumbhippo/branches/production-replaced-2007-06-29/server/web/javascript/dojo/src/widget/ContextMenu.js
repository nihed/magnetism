dojo.provide("dojo.widget.ContextMenu");

dojo.require("dojo.widget.*");
dojo.require("dojo.widget.DomWidget");

dojo.widget.ContextMenu = function(){
	dojo.widget.Widget.call(this);
	this.widgetType = "ContextMenu";
	this.isContainer = true;
	this.isOpened = false;
}

dojo.inherits(dojo.widget.ContextMenu, dojo.widget.Widget);
dojo.widget.tags.addParseTreeHandler("dojo:contextmenu");
