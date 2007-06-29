dojo.provide("dojo.widget.TimePicker");
dojo.require("dojo.widget.DomWidget");

dojo.widget.TimePicker = function(){
	dojo.widget.Widget.call(this);
	this.widgetType = "TimePicker";
	this.isContainer = false;

}

dojo.inherits(dojo.widget.TimePicker, dojo.widget.Widget);
dojo.widget.tags.addParseTreeHandler("dojo:timepicker");
