// This whole file is reloaded and object recreated for every toplevel window (but not tab!)
var Hippo = {
	branch : null,
	
	extension : null,
	
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
		// annoyingly, it seems insertItem does not change toolbar.currentset
		// which is a comma-separated list of item ids.
		// there's also "toolbar.currentSet" which seems to be different from 
		// the lowercase currentset attribute? anyway changing that didn't work
		currentset = toolbar.getAttribute("currentset");
		toolbar.setAttribute("currentset", currentset.replace("urlbar-container", "mugshot-button,urlbar-container"));
		
       	// save the toolbar settings
       	document.persist(toolbar.id, "currentset");
	},

	findAllBrowsers : function() {
		var browsers = [];
		var ifaces = Components.interfaces;
	  	var mediator =
	    	Components.classes["@mozilla.org/appshell/window-mediator;1"].
		      	getService(ifaces.nsIWindowMediator);
		// get all browser windows; empty string window type to get all windows period
		//"navigator:browser"
		var winEnum = mediator.getEnumerator("navigator:browser");
	  	while (winEnum.hasMoreElements()){
	    	var win = winEnum.getNext();
	    	
	    	//dump("window props\n===\n");
	    	//this.dumpProps(win);
	    	
	    	var contentNode = win.document.getElementById("content");
	    	var i;
	    	for (i = 0; i < contentNode.browsers.length; ++i) {
	    		var browser = contentNode.browsers[i];
				//dump("browser props\n===\n");
				//this.dumpProps(browser);
		    	browsers.push(browser);
	   		}
	  	}
	  	return browsers;
	},

	dumpProps : function(obj) {
		for (var prop in obj) {
			dump("  " + prop + " = " + obj[prop] + "\n");
		}
	},

	// frame all pages with given url with a frame for given postId
	framePages : function(postId, url) {
    	var browsers = this.findAllBrowsers();
    	dump("\nfound " + browsers.length + " open tabs\n");
		
		var i;
		for (i = 0; i < browsers.length; ++i) {
			var browser = browsers[i];
			var doc = browser.docShell.contentViewer.DOMDocument;
			dump("location = " + doc.location.href.toString() + "\n");
			if (doc.location.href.toString() == url) {
				browser.loadURI("http://mugshot.org/visit?post=" + postId);
			}
			//browser.loadURI("http://lwn.net");
		}
	},

	// on load of each toplevel window's chrome
    onLoad: function() {
		//dump("mugshot initialized = " + this.initialized + "\n");
		if (!this.initialized) {
        	this.initialized = true
        
        	var prefs = this.getPrefs();
        
        	this.extension = Components.classes["@mugshot.org/hippoExtension"]
	        	                        .getService(Components.interfaces.hippoIExtension);
		    if (!this.extension)
		    	alert("failed to load XPCOM control for Mugshot extension");
	    	this.extension.start("mugshot.org,dogfood.mugshot.org:9080,localinstance.mugshot.org:8080");
	        //alert("urls are: " + this.extension.servers);
        
    	    var addToToolbar = prefs.getBoolPref("addToToolbarOnStartup");
       		//alert("add to toolbar = " + addToToolbar);
	       	if (addToToolbar) {
    	   		this.addToolbarButton();
       			// don't fight user
	       		prefs.setBoolPref("addToToolbarOnStartup", false);
	    	}
	    }
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
    }
};

window.addEventListener("load", function(e) { Hippo.onLoad(e); }, false)
