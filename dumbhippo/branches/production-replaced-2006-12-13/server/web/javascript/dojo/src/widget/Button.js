dojo.provide("dojo.widget.Button");
dojo.require("dojo.widget.Widget");

dojo.require("dojo.widget.html.Button"); // hacked so jscompress can parse it

dojo.widget.tags.addParseTreeHandler("dojo:button");

dojo.widget.Button = function(){
	dojo.widget.Widget.call(this);

	this.widgetType = "Button";
	this.onClick = function(){ return; }
	this.isContainer = false;
}
dojo.inherits(dojo.widget.Button, dojo.widget.Widget);
