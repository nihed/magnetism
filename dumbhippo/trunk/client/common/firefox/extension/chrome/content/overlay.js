var HelloWorld = {
    onLoad: function() {
        // initialization code
        this.initialized = true
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

window.addEventListener("load", function(e) { HelloWorld.onLoad(e); }, false)
