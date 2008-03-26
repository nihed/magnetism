

Hippo.TabContext = function(browser) {
    this.browser = browser;

    var contentDeck = document.getElementById("hippoContentDeck");
    this.barBrowser = document.getElementById("hippoBarBrowser").cloneNode(true);
    this.barBrowser.style.display = '';
    this.barBrowser.setAttribute("tooltip", gBrowser.getAttribute("contenttooltip"))

    this._hookContentDOMActivate();

    contentDeck.appendChild(this.barBrowser);
},

Hippo.extend(Hippo.TabContext, {
    getContentWindow : function() {
        return this.barBrowser.contentWindow;
    },

    close : function() {
        var contentDeck = document.getElementById("hippoContentDeck");
        contentDeck.removeChild(this.barBrowser);
        this.barBrowser = null;
    },

    makeCurrent : function() {
        var contentDeck = document.getElementById("hippoContentDeck");
        contentDeck.selectedPanel = this.barBrowser;
    },

    setPost : function(baseUrl, postId) {
        this.barBrowser.loadURI(baseUrl + "/framer?postId=" + postId);
    },
	    
	_hookContentDOMActivate : function() {
		var me = this;
		this.barBrowser.addEventListener("DOMActivate",
			 function(e) { me._onContentDOMActivate(e); },
			 true);
	},

	_onContentDOMActivate : function (e) {
		if (e.target.tagName.toLowerCase() == "a") {
			var a = e.target;
			
			// If the target is specified, and looks like it refers to a new window
			// let the link click proceed normally. (_top, _self should be handled
			// as if there was no target)
			if (a.target != null) {
				if (a.target == "_blank" || a.target == "_new" || !(a.target.indexOf("_") == 0))
					return;
			}
			
			var barUrl = this.barBrowser.contentWindow.document.location.href;
			
			// We want javascript hrefs to execute normally, not be blocked, as
			// they would by the code below
			if (Hippo.uriSchemeIs(barUrl, a.href, "javascript"))
				return;

			e.preventDefault();
			e.stopPropagation();
			
			var resolved = Hippo.checkLoadUri(barUrl, a.href);
			if (resolved != null)
				this.browser.loadURI(resolved);
		}
    }
});
