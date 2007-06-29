dojo.provide("dojo.widget.TreeNode");
dojo.provide("dojo.widget.HtmlTreeNode");

dojo.require("dojo.event.*");
//dojo.require("dojo.html");
dojo.require("dojo.fx.html");
//dojo.require("dojo.dom");
dojo.require("dojo.widget.Tree");
//dojo.require("dojo.widget.Widget");
//dojo.require("dojo.widget.DomWidget");
dojo.require("dojo.widget.HtmlWidget");


// define the widget class
dojo.widget.HtmlTreeNode = function() {
    dojo.widget.HtmlWidget.call(this);

    this.widgetType = "TreeNode";
    this.templatePath = dojo.uri.dojoUri("src/widget/templates/TreeNode.html");
    this.templateCssPath = dojo.uri.dojoUri("src/widget/templates/TreeNode.css");
    this.isContainer = true;

    // the last node and with no children
    this.lastNodeLeafImgSrc = djConfig.baseRelativePath + "src/widget/templates/images/leaf-l.gif";
    // not the last node and with no children
    this.notLastNodeLeafImgSrc = djConfig.baseRelativePath + "src/widget/templates/images/leaf-t.gif";
    // the last node and with children
    this.lastNodeParentImgSrc = djConfig.baseRelativePath + "src/widget/templates/images/plus-l.gif";
    // not the last node and with children
    this.notLastNodeParentImgSrc = djConfig.baseRelativePath + "src/widget/templates/images/plus-t.gif";


    this.id = null;
    this.title = "";
    // the DOM node that holds the title and open / close controls
    this.nodeTitle = null;
    // the DOM text node with the title in it
    this.titleText = null;
    // the node which controls opening and closing the children (only exists when children are added)
    this.toggleControl = null;
    // the node which holds the toggle image.
    this.toggleImage = null;
    // the toggle strategy object which will toggle a parent node open and closed
    this.toggle = new dojo.widget.Tree.DefaultToggle();
    // the outer tree containing this node
    this.tree = null;
    // flag to hold whether this is the last node in the branch.
    this.isLastNode = true;

    this.initialize = function(args, frag){
        if (!this.id) {
            this.id = this.title;
        }

        this.toggleImage.src = this.lastNodeLeafImgSrc;
        this.toggleImage.alt = "";
    }

    var oldAddChild = this.addChild;
	this.addChild = function(widget, overrideContainerNode, pos, ref, insertIndex) {

        if (this.children && (this.children.length == 0)) {
            this.containerNode = document.createElement("div");
            this.domNode.appendChild(this.containerNode);
            this.containerNode.className = "TreeNodeBody";

        	if(this.isLastNode) {
        		this.toggleImage.src = this.lastNodeParentImgSrc;
        	} else {
        		this.toggleImage.src = this.notLastNodeParentImgSrc;
        	}

            dojo.event.connect(this.toggleImage, "onclick", this, "toggleOpened");
        } else if (this.children && (this.children.length > 0)) {
			this.children[this.children.length - 1].setNotLastNode();
        }

		return oldAddChild.call(this, widget, overrideContainerNode, pos, ref, insertIndex);
	}

    this.fillInTemplate = function () {
        this.titleText.appendChild(document.createTextNode(this.title));
//        this.nodeBody.appendChild(document.createElement("br"));
//        dojo.event.connect(this.nodeTitle, "onmouseover", this, "onMouseOver");
//        dojo.event.connect(this.nodeTitle, "onmouseout", this, "onMouseOut");
//        dojo.event.connect(this.nodeTitle, "onmousedown", this, "onMouseDown");
//        dojo.event.connect(this.nodeTitle, "onmouseup", this, "onMouseUp");
        dojo.event.connect(this.titleText, "onclick", this, "onClick");

    }

    this.setNotLastNode = function () {
        this.isLastNode = false;
        if (this.children && (this.children.length > 0)) {
            this.toggleImage.src = this.notLastNodeParentImgSrc;
        } else {
            this.toggleImage.src = this.notLastNodeLeafImgSrc;
        }
    }

    this.toggleOpened = function(e) {
        if (this.containerNode) {
            aToggle = this.getToggle();
            if (this.containerNode.style.display == "none" || this.containerNode.style.display == "") {
                aToggle.show(this.containerNode);
                this.onExpand(this, e);
            } else {
                aToggle.hide(this.containerNode);
                this.onCollapse(this, e);
            }
        }
    }

    this.getToggle = function () {
        if (this.parent && this.parent.getToggle) {
            return this.parent.getToggle();
        }
        return this.toggle;
    }

    this.onMouseOver = function (e) {
    }

    this.onMouseOut = function (e) {
    }

    this.onClick = function (e) {
        this.onSelect(this, e);
    }

    this.onMouseDown = function (e) {
    }

    this.onMouseUp = function (e) {
    }

    this.onSelect = function (item, e) {
        dojo.html.addClass(this.titleText,"TreeNodeSelected");
    }
    this.onTreeNodeSelected = function (item, e) {
		if (this.id != item.id) {
			dojo.html.removeClass(this.titleText,"TreeNodeSelected");
		}
    }
    this.onExpand = function (item, e) {
    	if(this.isLastNode) {
    		item.toggleImage.src = djConfig.baseRelativePath + "src/widget/templates/images/minus-l.gif";
    	} else {
    		item.toggleImage.src = djConfig.baseRelativePath + "src/widget/templates/images/minus-t.gif";
    	}
    }
    this.onCollapse = function (item, e) {
    	if(this.isLastNode) {
    		item.toggleImage.src = djConfig.baseRelativePath + "src/widget/templates/images/plus-l.gif";
    	} else {
    		item.toggleImage.src = djConfig.baseRelativePath + "src/widget/templates/images/plus-t.gif";
    	}
    }
}

// complete the inheritance process
dojo.inherits(dojo.widget.HtmlTreeNode, dojo.widget.HtmlWidget);

// make it a tag
dojo.widget.tags.addParseTreeHandler("dojo:TreeNode");

