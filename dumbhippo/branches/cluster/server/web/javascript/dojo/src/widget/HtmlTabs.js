dojo.provide("dojo.widget.Tabs");
dojo.provide("dojo.widget.HtmlTabs");

dojo.require("dojo.io.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.graphics.*");
dojo.require("dojo.dom");
dojo.require("dojo.html");

dojo.widget.HtmlTabs = function() {
	dojo.widget.HtmlWidget.call(this);

	this.widgetType = "Tabs";
	this.isContainer = true;

	this.templatePath = null; // prolly not
	this.templateCssPath = null; // maybe

	this.domNode = null;
	this.containerNode = null;

	this.tabs = [];
	this.panels = [];
	this.selected = -1;

	this.tabTarget = "";
	this.extractContent = false; // find the bits inside <body>
	this.parseContent = false; // parse externally loaded pages for widgets

	this.buildRendering = function(args, frag) {
		this.domNode = frag["dojo:"+this.widgetType.toLowerCase()]["nodeRef"];
		if(!this.domNode) { dj_error("HTMLTabs: No node reference"); }

		if(args["tabtarget"]) {
			this.tabtarget = args["tabtarget"];
			this.containerNode = document.getElementById(args["tabtarget"]);
		} else {
			this.containerNode = document.createElement("div");
			var next = this.domNode.nextSibling;
			if(next) {
				this.domNode.parentNode.insertBefore(this.containerNode, next);
			} else {
				this.domNode.parentNode.appendChild(this.containerNode);
			}
		}
		dojo.html.addClass(this.containerNode, "dojoTabPanelContainer");

		var li = dojo.dom.getFirstChildElement(this.domNode);
		while(li) {
			var a = li.getElementsByTagName("a").item(0);
			this.addTab(a);
			li = dojo.dom.getNextSiblingElement(li);
		}

		if(this.selected == -1) { this.selected = 0; }
		this.selectTab(null, this.tabs[this.selected]);
	}

	this.addTab = function(title, url, tabId, tabHandler) {
		// TODO: make this an object proper
		var panel = {
			url: null,
			title: null,
			isLoaded: false,
			id: null,
			isLocal: false
		};

		if(title && title.tagName && title.tagName.toLowerCase() == "a") {
			// init case
			var a = title;
			var li = a.parentNode;
			title = a.innerHTML;
			url = a.getAttribute("href");
			var id = null;
			var hash = url.indexOf("#");
			if(hash == 0 || (hash > 0 && location.href.split("#")[0] == url.split("#")[0])) {
				id = url.split("#")[1];
				dj_debug("setting local id:", id);
				url = "#" + id;
				panel.isLocal = true;
			} else {
				id = a.getAttribute("tabid");
			}

			panel.url = url;
			panel.title = title;
			panel.id = id || dojo.html.getUniqueId();
			dj_debug("panel id:", panel.id, "url:", panel.url);
		} else {
			// programmatically adding
			var li = document.createElement("li");
			var a = document.createElement("a");
			a.innerHTML = title;
			a.href = url;
			li.appendChild(a);
			this.domNode.appendChild(li);

			panel.url = url;
			panel.title = title;
			panel.id = tabId || dojo.html.getUniqueId();
			dj_debug("prg tab:", panel.id, "url:", panel.url);
		}

		if(panel.isLocal) {
			var node = document.getElementById(id);
			node.style.display = "none";
			this.containerNode.appendChild(node);
		} else {
			var node = document.createElement("div");
			node.style.display = "none";
			node.id = panel.id;
			this.containerNode.appendChild(node);
		}

		var handler = a.getAttribute("tabhandler") || tabHandler;
		if(handler) {
			this.setPanelHandler(handler, panel);
		}

		dojo.event.connect(a, "onclick", this, "selectTab");

		this.tabs.push(li);
		this.panels.push(panel);

		if(this.selected == -1 && dojo.html.hasClass(li, "current")) {
			this.selected = this.tabs.length-1;
		}
	}

	this.selectTab = function(e, target) {
		if(e) {
			if(e.target) {
				target = e.target;
				while(target && (target.tagName||"").toLowerCase() != "li") {
					target = target.parentNode;
				}
			}
			if(e.preventDefault) { e.preventDefault(); }
		}

		dojo.html.removeClass(this.tabs[this.selected], "current");

		for(var i = 0; i < this.tabs.length; i++) {
			if(this.tabs[i] == target) {
				dojo.html.addClass(this.tabs[i], "current");
				this.selected = i;
				break;
			}
		}

		var panel = this.panels[this.selected];
		if(panel) {
			this.getPanel(panel);
			this.hidePanels(panel);
			document.getElementById(panel.id).style.display = "";
		}
	}

	this.setPanelHandler = function(handler, panel) {
		var fcn = dojo.lang.isFunction(handler) ? handler : window[handler];
		if(!dojo.lang.isFunction(fcn)) {
			throw new Error("Unable to set panel handler, '" + handler + "' not a function.");
			return;
		}
		this["tabHandler" + panel.id] = function() {
			return fcn.apply(this, arguments);
		}
	}

	this.runPanelHandler = function(panel) {
		if(dojo.lang.isFunction(this["tabHandler" + panel.id])) {
			this["tabHandler" + panel.id](panel, document.getElementById(panel.id));
			return false;
		}
		return true;
		/*
		// in case we want to honor the return value?
		var ret = true;
		if(dojo.lang.isFunction(this["tabhandler" + panel.id])) {
			var val = this["tabhandler" + panel.id](this, panel);
			if(!dojo.lang.isUndefined(val)) {
				ret = val;
			}
		}
		return ret;
		*/
	}

	this.getPanel = function(panel) {
		if(this.runPanelHandler(panel)) {
			if(panel.isLocal) {
				// do nothing?
			} else {
				if(!panel.isLoaded || !this.useCache) {
					this.setExternalContent(panel, panel.url, this.useCache);
				}
			}
		}
	}

	this.setExternalContent = function(panel, url, useCache) {
		var node = document.getElementById(panel.id);
		node.innerHTML = "Loading...";

		var extract = this.extractContent;
		var parse = this.parseContent;

		dojo.io.bind({
			url: url,
			useCache: useCache,
			mimetype: "text/html",
			handler: function(type, data, e) {
				if(type == "load") {
					if(extract) {
						var matches = data.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
						if(matches) { data = matches[1]; }
					}
					node.innerHTML = data;
					panel.isLoaded = true;
					if(parse) {
						var parser = new dojo.xml.Parse();
						var frag = parser.parseElement(node, null, true);
						dojo.widget.getParser().createComponents(frag);
					}
				} else {
					node.innerHTML = "Error loading '" + panel.url + "' (" + e.status + " " + e.statusText + ")";
				}
			}
		});
	}

	this.hidePanels = function(except) {
		for(var i = 0; i < this.panels.length; i++) {
			if(this.panels[i] != except && this.panels[i].id) {
				var p = document.getElementById(this.panels[i].id);
				if(p) {
					p.style.display = "none";
				}
			}
		}
	}
}
dojo.inherits(dojo.widget.HtmlTabs, dojo.widget.HtmlWidget);

dojo.widget.tags.addParseTreeHandler("dojo:tabs");
