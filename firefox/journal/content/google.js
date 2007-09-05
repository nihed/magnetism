var GoogleSidebar = Class.create();
GoogleSidebar.prototype = {
  sidebarId: "GoogleSidebar",
  sidebarTitle: "Google",
  initialize: function() {
  },
  setupDom: function(div) {
    var ifr = document.createElement("iframe");
    ifr.setAttribute("id", "google-q");
    ifr.setAttribute("name", "google-q");
    ifr.setAttribute("frameborder", "0");
    ifr.setAttribute("scrolling", "no");
    ifr.setAttribute("src", "");
    ifr.setAttribute("marginwidth", "0");
    ifr.setAttribute("marginheight", "0");
    div.appendChild(ifr);
    div.style.display = "none";
  },
  searchQuery: function(q) {
    if (q) {
     frames["google-q"].location.href = "http://www.gnome.org/~clarkbw/google/?q=" + escape(q.strip());
      $("GoogleSidebar").style.display = "block";
    } else {
      $("GoogleSidebar").style.display = "none";
    }
  },
}

getJournalPageInstance().appendSidebar(new GoogleSidebar());
