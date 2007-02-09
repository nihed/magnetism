dojo.provide("dojo.widget.Dialog");
dojo.provide("dojo.widget.HtmlDialog");

dojo.require("dojo.widget.*");
dojo.require("dojo.graphics.*");
dojo.require("dojo.fx.*");
dojo.require("dojo.html");

dojo.widget.tags.addParseTreeHandler("dojo:dialog");

dojo.widget.HtmlDialog = function() {
	dojo.widget.HtmlDialog.superclass.constructor.call(this);

	this.widgetType = "Dialog";
	this.isContainer = true;

	this.templateString = '<div class="dojo-dialog">'
		+ '<span dojoAttachPoint="tabStart" dojoOnFocus="trapTabs" dojoOnBlur="clearTrap" tabindex="0"></span>'
		+ '<div dojoAttachPoint="containerNode"></div>'
		+ '<span dojoAttachPoint="tabEnd" dojoOnFocus="trapTabs" dojoOnBlur="clearTrap" tabindex="0"></span>'
		+ '</div>';

	this._scrollConnected = 0;

	// Only supports fade right now
	this.effect = "fade";
	this.effectDuration = 250;

	this.bg;
	this.bgColor = "black";
	this.bgOpacity = 0.4;
	this.followScroll = 1;

	var fromTrap = false;
	this.trapTabs = function(e) {
		if(e.target == this.tabStart) {
			if(fromTrap) {
				fromTrap = false;
			} else {
				fromTrap = true;
				this.tabEnd.focus();
			}
		} else if(e.target == this.tabEnd) {
			if(fromTrap) {
				fromTrap = false;
			} else {
				fromTrap = true;
				this.tabStart.focus();
			}
		}
	}

	this.clearTrap = function(e) {
		setTimeout(function() {
			fromTrap = false;
		}, 100);
	}

	this.postCreate = function(args, frag, parentComp) {
		dojo.html.body().appendChild(this.domNode);
		this.nodeRef = frag["dojo:"+this.widgetType.toLowerCase()]["nodeRef"];
		if(this.nodeRef) {
			this.setContent(this.nodeRef);
		}
		this.bg = document.createElement("div");
		this.bg.className = "dialogUnderlay";
		with(this.bg.style) {
			position = "absolute";
			left = top = "0px";
			width = dojo.html.getOuterWidth(dojo.html.body()) + "px";
			zIndex = 998;
			display = "none";
		}
		this.setBackgroundColor(this.bgColor);
		dojo.html.body().appendChild(this.bg);
		with(this.domNode.style) {
			position = "absolute";
			zIndex = 999;
			display = "none";
		}
	}

	this.setContent = function(content) {
		if(typeof content == "string") {
			this.containerNode.innerHTML = content;
		} else if(content.nodeType != undefined) {
			// dojo.dom.removeChildren(this.containerNode);
			this.containerNode.appendChild(content);
		} else {
			dojo.raise("Tried to setContent with unknown content (" + content + ")");
		}
	}

	this.setBackgroundColor = function(color) {
		if(arguments.length >= 3) {
			color = dojo.graphics.color.rgb2hex(arguments[0], arguments[1], arguments[2]);
		}
		this.bg.style.backgroundColor = color;
		return this.bgColor = color;
	}

	this.setBackgroundOpacity = function(op) {
		if(arguments.length == 0) { op = this.bgOpacity; }
		dojo.style.setOpacity(this.bg, op);
		return this.bgOpacity = dojo.style.getOpacity(this.bg);
	}

	this.sizeBackground = function() {
		var h = document.documentElement.scrollHeight || dojo.html.body().scrollHeight;
		this.bg.style.height = h + "px";
	}

	this.placeDialog = function() {
		var scrollTop = document.documentElement.scrollTop;
		var scrollLeft = document.documentElement.scrollLeft;
		// this is a candidate for helper function somewhere in dojo.style.*
		var W = dojo.html.getDocumentWidth();
		var H = dojo.html.getDocumentHeight();
		this.domNode.style.display = "block";
		var w = this.domNode.offsetWidth;
		var h = this.domNode.offsetHeight;
		this.domNode.style.display = "none";
		var L = scrollLeft + (W - w)/2;
		var T = scrollTop + (H - h)/2;
		with(this.domNode.style) {
			left = L + "px";
			top = T + "px";
		}
	}

	this.show = function() {
		this.setBackgroundOpacity();
		this.sizeBackground();
		this.placeDialog();
		switch((this.effect||"").toLowerCase()) {
			case "fade":
				this.bg.style.display = "block";
				this.domNode.style.display = "block";
				var _this = this;
				dojo.fx.fade(this.domNode, this.effectDuration, 0, 1, function(node) {
					if(dojo.lang.isFunction(_this.onShow)) {
						_this.onShow(node);
					}
				});
				break;
			default:
				this.bg.style.display = "block";
				this.domNode.style.display = "block";
				if(dojo.lang.isFunction(this.onShow)) {
					this.onShow(node);
				}
				break;
		}

		// FIXME: moz doesn't generate onscroll events for mouse or key scrolling (wtf)
		// we should create a fake event by polling the scrolltop/scrollleft every X ms.
		// this smells like it should be a dojo feature rather than just for this widget.

		if (this.followScroll && !this._scrollConnected){
			this._scrollConnected = 1;
			dojo.event.connect(window, "onscroll", this, "onScroll");
		}
	}

	this.hide = function() {
		switch((this.effect||"").toLowerCase()) {
			case "fade":
				this.bg.style.display = "none";
				var _this = this;
				dojo.fx.fadeOut(this.domNode, this.effectDuration, function(node) {
					node.style.display = "none";
					if(dojo.lang.isFunction(_this.onHide)) {
						_this.onHide(node);
					}
				});
				break;
			default:
				this.bg.style.display = "none";
				this.domNode.style.display = "none";
				if(dojo.lang.isFunction(this.onHide)) {
					this.onHide(node);
				}
				break;
		}

		if (this._scrollConnected){
			this._scrollConnected = 0;
			dojo.event.disconnect(window, "onscroll", this, "onScroll");
		}
	}

	this.setCloseControl = function(node) {
		dojo.event.connect(node, "onclick", this, "hide");
	}

	this.setShowControl = function(node) {
		dojo.event.connect(node, "onclick", this, "show");
	}

	this.onScroll = function(){
		this.placeDialog();
		this.domNode.style.display = "block";
	}
}
dojo.inherits(dojo.widget.HtmlDialog, dojo.widget.HtmlWidget);
