dojo.provide("dojo.widget.html.Button");
dojo.require("dojo.widget.HtmlWidget");
// removed require on dojo.widget.Button since it's a circular dep and confuses jscompress

dojo.widget.html.Button = function(){
	// mix in the button properties
	dojo.widget.Button.call(this);
	dojo.widget.HtmlWidget.call(this);
}
dojo.inherits(dojo.widget.html.Button, dojo.widget.HtmlWidget);
dojo.lang.extend(dojo.widget.html.Button, {

	templatePath: dojo.uri.dojoUri("src/widget/templates/HtmlButtonTemplate.html"),
	templateCssPath: dojo.uri.dojoUri("src/widget/templates/HtmlButtonTemplate.css"),

	// FIXME: freaking implement this already!
	foo: function(){ alert("bar"); },

	label: "huzzah!",
	labelNode: null,
	
	setLabel: function(){
		this.labelNode.innerHTML = this.label;
		// this.domNode.label = this.label;
	},

	fillInTemplate: function(){
		this.setLabel();
	},

	onFoo: function(){ }
});
