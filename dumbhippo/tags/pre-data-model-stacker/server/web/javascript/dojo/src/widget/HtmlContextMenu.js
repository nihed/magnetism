dojo.provide("dojo.widget.HtmlContextMenu");
dojo.require("dojo.html");
dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.ContextMenu");

dojo.widget.HtmlContextMenu = function(){
	dojo.widget.ContextMenu.call(this);
	dojo.widget.HtmlWidget.call(this);

	this.isShowing = 0;
	this.templatePath = dojo.uri.dojoUri("src/widget/templates/HtmlContextMenuTemplate.html");
	this.templateCssPath = dojo.uri.dojoUri("src/widget/templates/HtmlContextMenuTemplate.css");

	this.fillInTemplate = function(){
		// this.setLabel();
	}

	this.onShow = function(evt){
		if (this.isShowing){ this.onHide(); }
		this.isShowing = 1;

		evt.preventDefault();
		evt.stopPropagation();

		// FIXME: use whatever we use to do more general style setting?
		// FIXME: FIX this into something useful
		this.domNode.style.left = evt.clientX + "px";
		this.domNode.style.top = evt.clientY + "px";
		this.domNode.style.display = "block";
		dojo.event.connect(doc, "onclick", this, "onHide");
		return false;
	}
	
	this.onHide = function(){
		// FIXME: use whatever we use to do more general style setting?
		this.domNode.style.display = "none";
		dojo.event.disconnect(doc, "onclick", this, "onHide");
		this.isShowing = 0;
	}
	
	// FIXME: short term hack to show a single context menu in HTML
	// FIXME: need to prevent the default context menu...
	
	var doc = document.documentElement  || dojo.html.body();
	dojo.event.connect(doc, "oncontextmenu", this, "onShow");
}

dojo.inherits(dojo.widget.HtmlContextMenu, dojo.widget.HtmlWidget);
