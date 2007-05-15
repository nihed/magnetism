Hippo.WindowContext = function() {
	this.branch = null;
	this.initialized = false;
	this.observerService = null;
};

Hippo.extend(Hippo.WindowContext, {
	getPrefs : function() {
		if (!this.branch) {
			var manager = Components.classes["@mozilla.org/preferences-service;1"]
	                                .getService(Components.interfaces.nsIPrefBranch);
			this.branch = manager.getBranch("extensions.mugshot.");
		}
		return this.branch;
	},

	addToolbarButton : function() {
		// get location entry box
       	var urlbar = document.getElementById("urlbar-container");
       	if (!urlbar)
       		return;
       	var existingButton = document.getElementById("mugshot-button");
       	if (existingButton)
       		return;
       	var toolbar = urlbar.parentNode;
       	// http://xulplanet.com/references/elemref/ref_toolbar.html#prop_insertItem
       	// put our button right before the location entry box
		toolbar.insertItem("mugshot-button", urlbar);
		// toolbar.currentSet is the list of toolbar items currently in the
		// toolbar; this is distinct from the "currentset" attribute which is
        // the value that was read on startup and will be written back when
        // we call document.persist().
		currentset = toolbar.getAttribute("currentset");
		toolbar.setAttribute("currentset", toolbar.currentSet);
		
       	// save the toolbar settings
       	document.persist(toolbar.id, "currentset");
	},

	// frame all pages with given url with a frame for given postId; this only handles
	// pages within this toplevel; a separate copy of the extension will have been
	// loaded for other toplevel windows
	framePages : function(postId, url) {
    	var contentNode = document.getElementById("content");
    	var browsers = contentNode.browsers;
		
		var i;
		for (i = 0; i < browsers.length; ++i) {
			var browser = browsers[i];
			var doc = browser.docShell.contentViewer.DOMDocument;
			if (doc.location.href.toString() == url)
				browser.loadURI("http://mugshot.org/visit?post=" + postId);
		}
	},
	
	setBarCollapsed : function(collapsed) {
        var hippoContentDeck = document.getElementById("hippoContentDeck");        
        hippoContentDeck.collapsed = collapsed;
        var hippoContentSplitter = document.getElementById("hippoContentSplitter");
        hippoContentSplitter.collapsed = collapsed;
	},
	
	showBar : function(browser) {
        var doc = browser.docShell.contentViewer.DOMDocument;
	    var loc = doc.location;
	
        var params = Hippo.getParamsFromSearch(loc.search);
        if (params["post"] == null)
            return;
            
        var postId = params["post"];
        if (!postId.match(/[A-Za-z0-9]{14}/))
            return;
            
        var baseUrl = loc.protocol + "//" + loc.host

        if (browser.hippoTabContext == null) 
            browser.hippoTabContext = new Hippo.TabContext(browser);
        
        browser.hippoTabContext.setPost(baseUrl, params["post"]);
        browser.hippoTabContext.makeCurrent();
        
        this.setBarCollapsed(false);
	},

	// This check is to make sure that the framer content can't redirect the 
	// main window to javascript:, chrome:, file:, etc. The result is the
	// resolved URI using the fromUrl as the base (if url is relative),
	// or null if the security check failed.
	_checkLoadUri : function(fromUrl, url) {
	    try {
			const nsIIOService = Components.interfaces.nsIIOService;
			var ioServ = Components.classes["@mozilla.org/network/io-service;1"]
            	.getService(nsIIOService);
			var fromUri = ioServ.newURI(fromUrl, null /* charset */, null /* baseURI */ );
			var uri = ioServ.newURI(url, null, fromUri);

	        const nsIScriptSecurityManager = Components.interfaces.nsIScriptSecurityManager;
			var secMan = Components.classes["@mozilla.org/scriptsecuritymanager;1"]
                         .getService(nsIScriptSecurityManager);
			secMan.checkLoadURI(fromUri, uri, nsIScriptSecurityManager.DISALLOW_SCRIPT_OR_DATA);

			return uri.spec;
		} catch (e) {
			alert(e);
			return null;
	    }
	},

	hideBar : function(browser, barUrl, nextUrl) {
	    this.setBarCollapsed(true);
	    browser.hippoTabContext.close();
	    browser.hippoTabContext = null;

		var resolved;
	    nextUrl = Hippo.trim(nextUrl);
	    if (nextUrl != '')
			resolved = this._checkLoadUri(barUrl, nextUrl);

		if (resolved != null)
    		browser.loadURI(resolved);
	},
	
	addObservers : function() {
    	this.observerService = Components.classes["@mozilla.org/observer-service;1"].
            getService(Components.interfaces.nsIObserverService);
        // for third arg, true = weakref except I think we need to implement weak ref interface
        // before it works
        this.observerService.addObserver(this, "hippo-page-shared", false);
        this.observerService.addObserver(this, "hippo-open-bar", false);
        this.observerService.addObserver(this, "hippo-close-bar", false);
   	},

	_isFirefox20 : function() {
		var appInfo = Components.classes["@mozilla.org/xre/app-info;1"]
           		           .getService(Components.interfaces.nsIXULAppInfo);
	    var versionChecker = Components.classes["@mozilla.org/xpcom/version-comparator;1"]
                              .getService(Components.interfaces.nsIVersionComparator);

		return (versionChecker.compare(appInfo.version, "2.0") >= 0);
	},

    addTabListeners : function() {
    	var me = this;

		// Firefox-2.0 adds a better mechanism for watching tabs
    	if (this._isFirefox20()) {
    	    gBrowser.tabContainer.addEventListener("TabSelect",
    	                                           function(event) { me.onTabSelect(event) },
    	                                           false);
    	    gBrowser.tabContainer.addEventListener("TabClose",
    	                                           function(event) { me.onTabClose(event) },
    	                                           false);
    	} else {
        	gBrowser.mPanelContainer.addEventListener("select", 
                                              	      function(event) { me.onTabSelect(event) },
    	                                              false);	
        	gBrowser.mPanelContainer.addEventListener("DOMNodeRemoved", 
	                                                  function(event) { me.onTabCloseOld(event) }, 
	                                                  false);
	    }
    },
    
    getCurrentBrowser : function() {
    	return gBrowser.getBrowserAtIndex(gBrowser.mTabContainer.selectedIndex);
    },

    onTabSelect : function(event) {
        var browser = this.getCurrentBrowser();
        
        if (browser.hippoTabContext != null) {
            browser.hippoTabContext.makeCurrent();
            this.setBarCollapsed(false);
        } else {
            this.setBarCollapsed(true);
        }
    },
    
    doTabClose : function(browser) {
        // This is mainly to make sure we don't leak
        if (browser.hippoTabContext) {
    	    browser.hippoTabContext.close();
	        browser.hippoTabContext = null;
        }
    },
	
    onTabClose : function(event) {
        this.doTabClose(event.target.linkedBrowser);
    },
	
    onTabCloseOld : function(event) {
        if (event.relatedNode != gBrowser.mPanelContainer)
            return;
            
        var browser;
        if (event.target.localName == "vbox")
            browser = event.target.childNodes[1];
        if (browser == null);
            return;
            
        this.doTabClose(browser);
    },

    // on load of each toplevel window's chrome
    onLoad: function() {
		if (this.initialized)
		    return;
		
    	this.initialized = true;
        
    	var prefs = this.getPrefs();
	    var addToToolbar = prefs.getBoolPref("addToToolbarOnStartup");
	    
       	if (addToToolbar) {
	   		this.addToolbarButton();
   			// don't fight user
       		prefs.setBoolPref("addToToolbarOnStartup", false);
    	}
   
        this.addObservers(); 	
    	this.addTabListeners();
    },
    
    onUnload: function() {
    	dump("unloading mugshot extension for window\n");
	    this.observerService.removeObserver(this, "hippo-page-shared");
	    this.observerService.removeObserver(this, "hippo-open-bar");
	    this.observerService.removeObserver(this, "hippo-close-bar");
    },
    
    onToolbarButtonCommand: function(event) {
		var d = content.document;
		var url = encodeURIComponent(d.location.href);
		var title = encodeURIComponent(d.title);
		var top = (screen.availHeight - 400) / 2;
		var left = (screen.availWidth - 550) / 2;

        content.open('http://mugshot.org/sharelink?v=1&url='+url+'&title='+title+'&next=close',
                     '_NEW',
                     'menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=400,width=550,top='+top+',left='+left)
    },
    
    // implementation of IObserver
    observe : function(subject, topic, data) {
       	// this happens if we somehow fail to unregister our observer
    	if (typeof Components == 'undefined') {
    		dump("Components undefined in observer\n");
    		return;
    	}
    
    	dump("observing subject " + subject + " topic " + topic + " data " + data + "\n");
    	    	
    	if (topic == "hippo-page-shared") {
    		if (!data)
    			return;
    		var i = data.indexOf(",");
    		if (i != 14) { // length of guid
    			dump("comma in data at wrong index " + i + "\n");
    			return;
    		}
    		if (data.length < 16) {
    			dump("length of data too short " + data.length + "\n");    			
    			return;
    		}
    		var guid = data.substring(0,14);
    		var url = data.substring(15);
    		dump("framing pages guid='" + guid + "' url='" + url + "'\n");
    		this.framePages(guid, url);
    	} else if (topic == "hippo-open-bar") {
	    	var contentNode = document.getElementById("content");
	    	var i;
	    	
	    	for (i = 0; i < contentNode.browsers.length; ++i) {
	    		var browser = contentNode.browsers[i];
	    		if (browser.contentWindow == subject)
	    		    this.showBar(browser);
	   		}
        } else if (topic == "hippo-close-bar") {
	    	var contentNode = document.getElementById("content");
	    	var i;
	    	
	    	for (i = 0; i < contentNode.browsers.length; ++i) {
	    		var browser = contentNode.browsers[i];
	    		if (browser.hippoTabContext != null && browser.hippoTabContext.getContentWindow() == subject)
	    		    this.hideBar(browser, subject.document.location.href, data);
	   		}
        }
    },
    
    QueryInterface: function(aIID) {
	    var ifaces = Components.interfaces;
	    if (!aIID.equals(ifaces.nsIObserver) &&
    	    !aIID.equals(ifaces.nsISupports))
      		throw Components.results.NS_ERROR_NO_INTERFACE;
	    return this;
  	}
});
