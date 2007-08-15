var TopSitesSidebar = Class.create();
TopSitesSidebar.prototype = {
  sidebarId: "TopSitesSidebar",
  sidebarTitle: "Top Sites",
  initialize: function() {
  },
  setupDom: function(div) {
    var topSites = document.createElement("div");
    topSites.setAttribute("id", "topsites");
    div.appendChild(topSites);
  },
  redisplay: function(search) {
    var journal = getJournalInstance();
    var topSiteLimit = 10;
    var i = 0;
    var tsc = $("topsites");
    if (!tsc)
      return;
    while (tsc.firstChild) { tsc.removeChild(tsc.firstChild); }
    var ts = document.createElement("div");
    ts.style.display = "table";
    tsc.appendChild(ts);
    journal.iterTopSites(function (topsite) {
      if (search && !topsite.matches(search))
        return;
      if (i >= topSiteLimit) throw $break;
      i += 1;
      var a = document.createElement('a');
      a.href = topsite.url;
      a.style.display = "table-row";
      a.appendChild(getJournalPageInstance().renderJournalItemContent(topsite));
      ts.appendChild(a);
    });
  }
}

getJournalPageInstance().appendSidebar(new TopSitesSidebar());
