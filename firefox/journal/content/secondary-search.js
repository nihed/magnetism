var SecondarySearch = Class.create();
SecondarySearch.prototype = {
  sidebarId: "SecondarySearch",
  sidebarTitle: "Other Searches",
  initialize: function() {
    this.SEARCH_SERVICE = Components.classes["@mozilla.org/browser/search-service;1"].getService(Components.interfaces.nsIBrowserSearchService);
  },
  setupDom: function(div) {
    div.style.display = "none";
    var ul = document.createElement("ul");
    ul.setAttribute("id", "secondary-search");
    div.appendChild(ul);
  },
  searchQuery: function(q) {
    if (q) {
      var engines = this.SEARCH_SERVICE.getEngines(Object());
      var ss = $("secondary-search");
      while (ss.firstChild) { ss.removeChild(ss.firstChild); };

      for (var i = 1; i < engines.length; i++) {
        var engine = engines[i];
        var li = document.createElement("li");
        li.setAttribute("class", "search-engine");
        var a = document.createElement("a");
        if (engine.iconURI) {
          var img = document.createElement("img");
          img.className = "search-engine";
          img.src = engine.iconURI.spec;
          a.appendChild(img);
        }
        a.appendChild(document.createTextNode(engine.name));
        a.setAttribute("href", engine.getSubmission(q, null).uri.spec);
        a.setAttribute("id", "altsearch-" + i);
        a.setAttribute("class", "search-engine");
        li.appendChild(a);
        li.appendChild(document.createTextNode(" [Ctrl-" + i + "]"));
        ss.appendChild(li);
      }

      $(this.sidebarId).style.display = "block";
    } else {
      $(this.sidebarId).style.display = "none";
    }
  },
}


getJournalPageInstance().appendSidebar(new SecondarySearch());
