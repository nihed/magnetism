dojo.provide("dojo.widget.FloatingPane");
dojo.provide("dojo.widget.HtmlFloatingPane");

//
// this widget provides a window-like floating pane
//

dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dojo.style");
dojo.require("dojo.dom");
dojo.require("dojo.widget.HtmlLayoutPane");

dojo.widget.HtmlFloatingPane = function(){
	dojo.widget.HtmlLayoutPane.call(this);
}

dojo.inherits(dojo.widget.HtmlFloatingPane, dojo.widget.HtmlLayoutPane);

dojo.lang.extend(dojo.widget.HtmlFloatingPane, {
	widgetType: "FloatingPane",

	isContainer: true,
	containerNode: null,
	domNode: null,
	clientPane: null,
	dragBar: null,
	dragOrigin: null,
	posOrigin: null,
	maxPosition: null,
	title: 'Untitled',
	constrainToContainer: 0,
	templateCssPath: dojo.uri.dojoUri("src/widget/templates/HtmlFloatingPane.css"),


	fillInTemplate: function(){

		if (this.templateCssPath) {
			dojo.style.insertCssFile(this.templateCssPath, null, true);
		}

		dojo.html.addClass(this.domNode, 'dojoFloatingPane');

		// this is our client area

		var elm = document.createElement('div');
		dojo.dom.moveChildren(this.domNode, elm, 0);
		dojo.html.addClass(elm, 'dojoFloatingPaneClient');
		this.clientPane = this.createPane(elm, 'client');
		this.clientPane.ownerPane = this;


		// this is our chrome

		var elm = document.createElement('div');
		elm.appendChild(document.createTextNode(this.title));
		dojo.html.addClass(elm, 'dojoFloatingPaneDragbar');
		this.dragBar = this.createPane(elm, 'top');
		this.dragBar.ownerPane = this;

		dojo.html.disableSelection(this.dragBar.domNode);
		dojo.event.connect(this.dragBar.domNode, 'onmousedown', this, 'onMyDragStart');

		this.layoutSoon();
	},

	postCreate: function(args, fragment, parentComp){

		// attach our children

		for(var i=0; i<this.children.length; i++){
			if (this.children[i].ownerPane != this){
				this.clientPane.domNode.appendChild(this.children[i].domNode);
			}
		}
	},

	createPane: function(node, align){

		var pane = dojo.widget.fromScript("LayoutPane", { layoutAlign: align }, node);

		this.addPane(pane);

		return pane;
	},

	onMyDragStart: function(e){

		this.dragOrigin = {'x': e.pageX, 'y': e.pageY};
		this.posOrigin = {'x': dojo.style.getNumericStyle(this.domNode, 'left'), 'y': dojo.style.getNumericStyle(this.domNode, 'top')};

		if (this.constrainToContainer){
			// get parent client size...

			if (this.domNode.parentNode.nodeName.toLowerCase() == 'body'){
				var parentClient = {
					'w': window.innerWidth,
					'h': window.innerHeight
				};
			}else{
				var parentClient = {
					'w': this.domNode.parentNode.offsetWidth - dojo.style.getBorderWidth(this.domNode.parentNode),
					'h': this.domNode.parentNode.offsetHeight - dojo.style.getBorderHeight(this.domNode.parentNode)
				};
			}

			this.maxPosition = {
				'x': parentClient.w - dojo.style.getOuterWidth(this.domNode),
				'y': parentClient.h - dojo.style.getOuterHeight(this.domNode)
			};
		}

		dojo.event.connect(window, 'onmousemove', this, 'onMyDragMove');
		dojo.event.connect(window, 'onmouseup', this, 'onMyDragEnd');
	},

	onMyDragMove: function(e){

		var x = this.posOrigin.x + (e.pageX - this.dragOrigin.x);
		var y = this.posOrigin.y + (e.pageY - this.dragOrigin.y);

		if (this.constrainToContainer){
			if (x < 0){ x = 0; }
			if (y < 0){ y = 0; }
			if (x > this.maxPosition.x){ x = this.maxPosition.x; }
			if (y > this.maxPosition.y){ y = this.maxPosition.y; }
		}

		this.domNode.style.left = x + 'px';
		this.domNode.style.top  = y + 'px';
	},

	onMyDragEnd: function(e){

		dojo.event.disconnect(window, 'onmousemove', this, 'onMyDragMove');
		dojo.event.disconnect(window, 'onmouseup', this, 'onMyDragEnd');
	}
	
});

dojo.widget.tags.addParseTreeHandler("dojo:FloatingPane");
