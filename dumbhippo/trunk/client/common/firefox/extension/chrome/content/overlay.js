var Hippo = {
	branch : null,
	
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

    onLoad: function() {
        // initialization code
        this.initialized = true
        
        var prefs = this.getPrefs();
        
        var addToToolbar = prefs.getBoolPref("addToToolbarOnStartup");
       	//alert("add to toolbar = " + addToToolbar);
       	if (addToToolbar) {
       		this.addToolbarButton();
       		// don't fight user
	       	prefs.setBoolPref("addToToolbarOnStartup", false);
	    }
    },

    onMenuItemCommand: function() {
        alert("HERE")
    },
    
    onToolbarButtonCommand: function(event) { 
	var d = content.document
	var url = encodeURIComponent(d.location.href)
	var title = encodeURIComponent(d.title)
	var top = (screen.availHeight - 400) / 2
	var left = (screen.availWidth - 550) / 2

        content.open('http://fresnel.dumbhippo.com:8080/sharelink?v=1&url='+url+'&title='+title+'&next=close',
                     '_NEW',
                     'menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=400,width=550,top='+top+',left='+left)
    }
};

window.addEventListener("load", function(e) { Hippo.onLoad(e); }, false)
