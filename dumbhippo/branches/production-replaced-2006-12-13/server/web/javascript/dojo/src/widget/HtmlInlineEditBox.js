dojo.provide("dojo.widget.InlineEditBox");
dojo.provide("dojo.widget.HtmlInlineEditBox");

dojo.require("dojo.widget.*");
dojo.require("dojo.fx.*");
dojo.require("dojo.graphics.color");
dojo.require("dojo.string");
dojo.require("dojo.style");
dojo.require("dojo.html");

dojo.widget.HtmlInlineEditBox = function() {
	dojo.widget.HtmlWidget.call(this);

	this.templatePath = dojo.uri.dojoUri("src/widget/templates/HtmlInlineEditBox.html");
	this.templateCssPath = dojo.uri.dojoUri("src/widget/templates/HtmlInlineEditBox.css");
	this.widgetType = "InlineEditBox";

	this.form = null;
	this.editBox = null;
	this.edit = null;
	this.text = null;
	this.textarea = null;
	this.mode = "text";
	this.storage = document.createElement("span");

	this.minWidth = 100; //px. minimum width of edit box
	this.minHeight = 200; //px. minimum width of edit box, if it's a TA

	this.editing = false;
	this.textValue = "";
	this.defaultText = "";
	this.doFade = false;

	this.history = [];

	this.onSave = function(newValue, oldValue){};
	this.onUndo = function(value){};

	// overwrite buildRendering so we don't clobber our list
	this.buildRendering = function(args, frag) {
		this.nodeRef = frag["dojo:"+this.widgetType.toLowerCase()]["nodeRef"];
		var node = this.nodeRef;
		if(node.normalize) { node.normalize(); }

		dojo.widget.buildAndAttachTemplate(this);
		
		var initialText = ""
		// It had better be a text node...		
		if (node.firstChild)
			initialText = node.firstChild.nodeValue
		while (node.firstChild) {
		  node.removeChild(node.firstChild)
		}

		this.editable = document.createElement("span");
		this.setText(initialText)
		node.appendChild(this.editable);

		// delay to try and show up before stylesheet
		var _this = this;
		setTimeout(function() {
			_this.editable.appendChild(_this.edit);
		}, 30);

		dojo.event.connect(this.editable, "onmouseover", this, "mouseover");
		dojo.event.connect(this.editable, "onmouseout", this, "mouseout");
		dojo.event.connect(this.editable, "onclick", this, "beginEdit");

		this.fillInTemplate(args, frag);
	}

	this.mouseover = function(e) {
		if(!this.editing) {
			dojo.html.addClass(this.editable, "editableRegion");
			if(this.mode == "textarea"){
				dojo.html.addClass(this.editable, "editableTextareaRegion");
			}
		}
	}

	this.mouseout = function(e) {
		// if((e)&&(e.target != this.domNode)){ return; }
		if(!this.editing) {
			dojo.html.removeClass(this.editable, "editableRegion");
			dojo.html.removeClass(this.editable, "editableTextareaRegion");
		}
	}

	this.beginEdit = function(e) {
		if(this.editing) { return; }
		this.mouseout();
		this.editing = true;

		var ee = this[this.mode.toLowerCase()];

		ee.style.display = "";
		ee.value = dojo.string.trim(this.textValue);
		ee.style.fontSize = dojo.style.getStyle(this.editable, "font-size");
		ee.style.fontWeight = dojo.style.getStyle(this.editable, "font-weight");
		ee.style.fontStyle = dojo.style.getStyle(this.editable, "font-style");
		//this.text.style.fontFamily = dojo.dom.getStyle(this.editable, "font-family");

		ee.style.width = Math.max(dojo.html.getInnerWidth(this.editable), this.minWidth) + "px";
		// ee.style.width = "100%";

		if(this.mode.toLowerCase()=="textarea"){
			ee.style.display = "block";
			ee.style.height = Math.max(dojo.html.getInnerHeight(this.editable), this.minHeight) + "px";
		}
		this.editable.style.display = "none";
		this.nodeRef.appendChild(this.form);
		ee.select();
	}

	this.saveEdit = function(e) {
		e.preventDefault();
		e.stopPropagation();
		var ee = this[this.mode.toLowerCase()];
		if((this.textValue != ee.value)&&
			(dojo.string.trim(ee.value) != "")){
			this.doFade = true;
			this.history.push(this.textValue);
			this.onSave(ee.value, this.textValue);
			this.setText(ee.value);
		} else {
			this.doFade = false;
		}
		this.finishEdit(e);
	}

	this.cancelEdit = function(e) {
		if(!this.editing) { return false; }
		this.editing = false;
		this.nodeRef.removeChild(this.form);
		this.editable.style.display = "";
		return true;
	}

	this.finishEdit = function(e) {
		if(!this.cancelEdit(e)) { return; }
		if(this.doFade) {
			dojo.fx.highlight(this.editable, dojo.graphics.color.hex2rgb("#ffc"), 700, 300);
		}
		this.doFade = false;
	}

	this.setText = function(txt){
		this.textValue = txt
		while(this.editable.firstChild){
			this.editable.removeChild(this.editable.firstChild)
		}		
		this.editable.appendChild(document.createTextNode(txt))
	}

	this.undo = function() {
		if(this.history.length > 0) {
			var value = this.history.pop();
			this.setText(value)
			this.onUndo(value);
		}
	}
}
dojo.inherits(dojo.widget.HtmlInlineEditBox, dojo.widget.HtmlWidget);
dojo.widget.tags.addParseTreeHandler("dojo:inlineeditbox");
