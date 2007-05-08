Hippo.TabContext = function(browser) {
    this.browser = browser;

    var contentDeck = document.getElementById("hippoContentDeck");
    this.barBrowser = document.getElementById("hippoBarBrowser").cloneNode(true);
    this.barBrowser.style.display = '';

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
    }
});
