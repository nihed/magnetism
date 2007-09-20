/* ***** BEGIN LICENSE BLOCK ****
 *   Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Firefox Journal.
 *
 * The Initial Developer of the Original Code is
 * Red Hat, Inc.
 * Portions created by the Initial Developer are Copyright (C) 2007
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 * 
 * ***** END LICENSE BLOCK ***** */

const Ci = Components.interfaces;
const Cc = Components.classes;

//const JOURNAL_SERVICE = Cc["@redhat.com/journalhome;1"].getService(Ci["nsIJournalHome"]);
const HISTORY_SERVICE = Cc["@mozilla.org/browser/nav-history-service;1"].getService(Ci.nsINavHistoryService);
const IO_SERVICE = Cc["@mozilla.org/network/io-service;1"].getService(Ci.nsIIOService);
const ANNOTATION_SERVICE = Cc["@mozilla.org/browser/annotation-service;1"].getService(Ci.nsIAnnotationService);
const SEARCH_SERVICE = Cc["@mozilla.org/browser/search-service;1"].getService(Ci.nsIBrowserSearchService);
const TAGGING_SERVICE = Cc["@mozilla.org/browser/tagging-service;1"].getService(Ci.nsITaggingService);

const JOURNAL_CHROME = "chrome://firefoxjournal/content/journal.html"; 

const BLANK_FAVICON = "chrome://mozapps/skin/places/defaultFavicon.png"
const FIREFOX_FAVICON = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAAK/INwWK6QAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAHWSURBVHjaYvz//z8DJQAggJiQOe/fv2fv7Oz8rays/N+VkfG/iYnJfyD/1+rVq7ffu3dPFpsBAAHEAHIBCJ85c8bN2Nj4vwsDw/8zQLwKiO8CcRoQu0DxqlWrdsHUwzBAAIGJmTNnPgYa9j8UqhFElwPxf2MIDeIrKSn9FwSJoRkAEEAM0DD4DzMAyPi/G+QKY4hh5WAXGf8PDQ0FGwJ22d27CjADAAIIrLmjo+MXA9R2kAHvGBA2wwx6B8W7od6CeQcggKCmCEL8bgwxYCbUIGTDVkHDBia+CuotgACCueD3TDQN75D4xmAvCoK9ARMHBzAw0AECiBHkAlC0Mdy7x9ABNA3obAZXIAa6iKEcGlMVQHwWyjYuL2d4v2cPg8vZswx7gHyAAAK7AOif7SAbOqCmn4Ha3AHFsIDtgPq/vLz8P4MSkJ2W9h8ggBjevXvHDo4FQUQg/kdypqCg4H8lUIACnQ/SOBMYI8bAsAJFPcj1AAEEjwVQqLpAbXmH5BJjqI0gi9DTAAgDBBCcAVLkgmQ7yKCZxpCQxqUZhAECCJ4XgMl493ug21ZD+aDAXH0WLM4A9MZPXJkJIIAwTAR5pQMalaCABQUULttBGCCAGCnNzgABBgAMJ5THwGvJLAAAAABJRU5ErkJggg==";

const DAY_MS = 24 * 60 * 60 * 1000;

function LOG(msg) {
  var dl = $("debuglog");
  dl.appendChild(document.createTextNode(msg));
  dl.appendChild(document.createElement("br"));
}

// Used to recognize search URLs, so we can display them as a "search" with icon
var searchMapping = $H({
  // TODO: are these names returned translated?  Also the URLs here probably need translation
  "Google Web Search": {"urlStart": "http://www.google.com/search",
             "qparams": ["q"]},
  "Google Code Search": {"urlStart": "http://www.google.com/codesearch",
             "qparams": ["q"]},
  "Google Maps Search": {"urlStart": "http://maps.google.com/maps",
             "qparams": ["q"]},
  "Yahoo": {"urlStart": "http://search.yahoo.com/search",
            "qparams": ["p"]},
  "Amazon.com": {"urlStart": "http://www.amazon.com/",
                 "qparams": ["field-keywords"]},
  "Creative Commons": {"urlStart": "http://search.creativecommons.org/",
                       "qparams": ["q"]},
  "eBay": {"urlStart": "http://search.ebay.com/",
           "qparams": ["satitle", "query"]},
  "Netflix": {"urlStart": "http://www.netflix.com/Search",
           "qparams": ["v1"]},
});

/***** The Journal *****/

var JournalEntry = Class.create();
JournalEntry.prototype = {
  initialize: function(histitem) {
    var me = this;
    this.histitem = histitem;

    this.url = histitem.url;
    this.date = histitem.lastVisitDate;
    this.displayUrl = formatUtils.ellipsize(histitem.url.split("?")[0], 50);
    this.title = this.histitem.title;
    this.actionIcon = null;
    this.action = 'visited';
    
    var queryParams = histitem.url.toQueryParams();
    
    var engines = SEARCH_SERVICE.getEngines(Object()); /* NS strongly desires an Out argument to be an object */
    searchMapping.each(function (kv) {
      if (!me.url.startsWith(kv[1]['urlStart']))
        return;
      var qps = kv[1]['qparams'];
      var qp = null;
      for (var i = 0; i < qps.length; i++) {
        if (queryParams[qps[i]]) {
          qp = qps[i];
          break;
        }
      }
      if (!qp)
        return;

      me.title = decodeURIComponent(queryParams[qp].replace(/\+/g," "));
      me.action = 'search';

      var engine = null;
      engines.each(function (eng) {
        if (eng.name == kv[0]) {
          engine = eng;
          throw $break;
        }
      });
      if (engine)
        me.actionIcon = engine.iconURI.spec;
    });
  },
  matches: function (q) {
    return this['url'].indexOf(q) >= 0 || this['title'].toLowerCase().indexOf(q) >= 0;
  },
} 

var Journal = Class.create();
Journal.prototype = {
  initialize: function () {
  },
  _getBaseQueryOptions: function() {
    var options = HISTORY_SERVICE.getNewQueryOptions();
    options.resultType = options.RESULTS_AS_URI;
    options.setGroupingMode([options.GROUP_BY_DAY], 1);
    options.sortingMode = options.SORT_BY_DATE_DESCENDING;
    return options;
  },
  getLastHistoryDay: function() {
    var options = HISTORY_SERVICE.getNewQueryOptions();
    options.maxResults = 1;
    options.sortingMode = options.SORT_BY_DATE_DESCENDING;
    var histq = HISTORY_SERVICE.getNewQuery();    
   
    var lastHistoryItemResults = HISTORY_SERVICE.executeQuery(histq, options);
    var lastHistoryTime;
    lastHistoryItemResults.root.containerOpen = true;
    if (!lastHistoryItemResults.root.hasChildren) {
      lastHistoryItemResults.root.containerOpen = false;
      return null;
    }
    var lastHistoryItem = lastHistoryItemResults.root.getChild(0);
    lastHistoryTime = lastHistoryItem.time;
    lastHistoryItemResults.root.containerOpen = false;

    histq = HISTORY_SERVICE.getNewQuery();
    options = this._getBaseQueryOptions();
    histq.beginTimeReference = histq.TIME_RELATIVE_EPOCH;
    histq.beginTime = lastHistoryTime  - (24 * 60 * 60 * 1000000); // a day
    histq.endTimeReference = histq.TIME_RELATIVE_EPOCH;
    histq.endTime = lastHistoryTime;

    return HISTORY_SERVICE.executeQuery(histq, options);
  },
  search: function(q, limit) {
    var options = this._getBaseQueryOptions();
    options.resultType = options.RESULTS_AS_URI;
    var histq = HISTORY_SERVICE.getNewQuery();
    histq.searchTerms = q;
    // FIXME - uncomment this when https://bugzilla.mozilla.org/show_bug.cgi?id=394508 is in
    // options.maxResults = limit;
    return HISTORY_SERVICE.executeQuery(histq, options);
  },
  searchTopSites: function(q) {
    var options = this._getBaseQueryOptions();
    options.resultType = options.RESULTS_AS_URI;
    options.setGroupingMode([], 0);
    options.sortingMode = options.SORT_BY_VISITCOUNT_DESCENDING;
    var histq = HISTORY_SERVICE.getNewQuery();
    if (q) histq.searchTerms = q;
    histq.minVisits = 3; /* picking a static value for this is a little silly */
    //options.maxResults = 5;
    return HISTORY_SERVICE.executeQuery(histq, options);
  },
}

var theJournal;
var getJournalInstance = function() {
  if (theJournal == null)
    theJournal = new Journal();
  return theJournal;
}

var JournalPage = Class.create();
JournalPage.prototype = {
  initialize: function() {
    this.sidebars = $A();
    this.searchTimeout = null;
    this.searchQueryTimeout = null;
    this.searchValue = null;
    this.targetHistoryItem = null;
  },
  appendDaySet: function(dayset) {
    dayset.QueryInterface(Ci.nsINavHistoryContainerResultNode);
    dayset.containerOpen = true;
    var date = new Date(dayset.getChild(0).time/1000);
    var today = new Date();

    var content = $('history');    
    var headernode = document.createElement('h4');
    headernode.className = 'date';
    if (dateTimeUtils.getLocalDayOffset(today) == dateTimeUtils.getLocalDayOffset(date))
      headernode.appendChild(document.createTextNode("Today"))
    else
      headernode.appendChild(document.createTextNode(formatUtils.monthName(date.getMonth()) + " " + formatUtils.pad(date.getDate()) + " " + date.getFullYear()));
    content.appendChild(headernode);
    var histnode = document.createElement('div');
    histnode.className = 'set';
    content.appendChild(histnode);

    for (var i = 0; i < dayset.childCount; i++) {
      histnode.appendChild(this.renderJournalItem(dayset.getChild(i)));
    }
    dayset.containerOpen = false;
  },
  appendTopSites: function(siteset) {

    siteset.root.containerOpen = true;
    var count = (siteset.root.childCount > 5)? 5 : siteset.root.childCount;
    if (count == 0) return;

    var content = $('top-sites');    
    var headernode = document.createElement('h4');
    headernode.appendChild(document.createTextNode("Top Sites"));
    content.appendChild(headernode);
    var sitenode = document.createElement('div');
    sitenode.className = 'set';

    for (var i = 0; i < count; i++) {
      sitenode.appendChild(this.renderTopItem(siteset.root.getChild(i), (i == 0)));
    }

    siteset.root.containerOpen = false;

    content.appendChild(sitenode);
  },
  renderTopItem: function(entry, isTarget) {
    var me = this;  

    var item = document.createElement('a');
    item.href = entry.uri;
    item.className = 'item';
    item.setAttribute('tabindex', 1); 

    if (isTarget) {
      item.setAttribute('id', 'default-target-item');
    }

    item.addEventListener("focus", function(e) { me.onResultFocus(e, true); }, false);
    item.addEventListener("blur", function(e) { me.onResultFocus(e, false); }, false);             
        
    this.renderJournalItemContent(entry, item);

    return item;
  },
  renderJournalItemContent: function(entry, item) {
    var iconSection = document.createElement('div');
    iconSection.className = 'favicon';
    var a = document.createElement('a');
    a.href = entry.uri;
    iconSection.appendChild(a);
    var img = document.createElement('img');
    img.className = 'favicon-img';
    if (entry.icon)
      img.src = entry.icon.spec;
    else
      img.src = BLANK_FAVICON;
    a.appendChild(img);
    item.appendChild(iconSection);     

    var urlSection = document.createElement('div');
    urlSection.className = 'urls';
    var titleDiv = document.createElement('div');
    titleDiv.appendChild(domUtils.createSpanText(entry.title,'title'));
    urlSection.appendChild(titleDiv);
    var hrefDiv = document.createElement('div');
    hrefDiv.appendChild(domUtils.createSpanText(formatUtils.ellipsize(entry.uri, 80), 'url'));
    urlSection.appendChild(hrefDiv);
    item.appendChild(urlSection);
  },
  renderJournalItem: function(entry) {
    var me = this;  

    var item = document.createElement('a');
    item.href = entry.uri;
    item.className = 'item';
    item.setAttribute('tabindex', 1); 

    item.addEventListener("focus", function(e) { me.onResultFocus(e, true); }, false);
    item.addEventListener("blur", function(e) { me.onResultFocus(e, false); }, false);             
    
    if (entry.time) {
      var dateTime = new Date(entry.time/1000);
      var timeText = formatUtils.twelveHour(dateTime.getHours()) + ":" + formatUtils.pad(dateTime.getMinutes()) + " " + formatUtils.meridiem(dateTime.getHours());
      item.appendChild(domUtils.createSpanText(timeText, 'time'));
    } 
    
    this.renderJournalItemContent(entry, item);

    return item;
  },
  renderSearchInfoBar: function(q, searchIsWeblink) {
      var me = this;
      var node = $("search-info-bar");
      while (node.firstChild) { node.removeChild(node.firstChild); };
 
      node.appendChild(domUtils.createSpanText("Searching history for ", "search-pre-term"));
      node.appendChild(domUtils.createSpanText(q,"search-term"));
      var clearSearch = document.createElement("a");
      clearSearch.addEventListener("click", function() { me.clearSearch(); }, false);
      clearSearch.href = "javascript:void(0);";
      clearSearch.className = "clear-search";
      clearSearch.setAttribute("accesskey", "c");
      clearSearch.setAttribute("title", "Clear this search [ESC]");
      clearSearch.appendChild(document.createTextNode("[clear]"));
      node.appendChild(clearSearch);
  },
  getAction: function(entry) {
    try {
      /* FIXME: need to get the correct annotation for the uri */
      return entry.action || "visited";
/*
      var flags = {}, exp = {}, mimeType = {}, type = {};
      ANNOTATION_SERVICE.getPageAnnotationInfo(uri, "journal/action", flags, exp, mimeType, type);
      LOG("flags: " + flags.value + " exp: " + exp.value + " mimeType: " + mimeType.value + " type: " + type.value);
      var action = ANNOTATION_SERVICE.getPageAnnotationString(uri, "journal/action");
      ANNOTATION_SERVICE.removePageAnnotations(uri);
*/
      var uri = new String(entry.uri);
      LOG("uri: " + uri);      
      LOG("action retrieved: " + ANNOTATION_SERVICE.getPageAnnotation(uri, "journal/action"));
      $("debuglog").appendChild(document.createElement("br"));

      /* we're not getting the correct action here, instead we're getting the action from first uri returned every time */
      var annon = ANNOTATION_SERVICE.getPageAnnotation(uri, "journal/action");
      if (!annon) throw annon;
      return annon;
    } catch (e) {
      return this._getAction(entry);
    }
    return "!visited";
  },
  _getAction: function(entry) {
      var action = "visited";
      var uri = new String(entry.uri);
      var queryParams = uri.toQueryParams();

      searchMapping.each(function (kv) {
        if (!uri.startsWith(kv[1]['urlStart']))
          return;

        var qps = kv[1]['qparams'];
        var qp = null;
        for (var i = 0; i < qps.length; i++) {
          if (queryParams[qps[i]]) {
            qp = qps[i];
            break;
          }
        }

        if (!qp) 
          return;

        action = "search";

        LOG("qp: " + qp + " params: " + queryParams[qp]);

/*      FIXME: commented out for now
        var engines = SEARCH_SERVICE.getEngines(Object());
        var engine = null;
        engines.each(function (eng) {
          if (eng.name == kv[0]) {
            engine = eng;
            throw $break;
          }
*/
      });

      LOG("action: " + action);

      try {
        ANNOTATION_SERVICE.setPageAnnotation( uri, "journal/action", action, 0, 0 ); // ANNOTATION_SERVICE.EXPIRE_WITH_HISTORY );
      } catch(e) { LOG("action error: " + e + " : " + uri); }

      /* We're retrieving the correct action here after setting it above */
      LOG("uri: " + uri);
      LOG("action saved: " + ANNOTATION_SERVICE.getPageAnnotation(uri, "journal/action"));
      $("debuglog").appendChild(document.createElement("br"));

    return action;
  },
  setAsTargetItem: function (node) {
    node.addClassName("target-item");
  },
  unsetAsTargetItem: function (node) {
    node.removeClassName("target-item");
  },
  onResultFocus: function(e, focused) {
    if (focused) {
      var defTarget = document.getElementById("default-target-item");
      if (defTarget) 
        this.unsetAsTargetItem(defTarget); 
      this.setAsTargetItem(e.target);
    } else {
      this.unsetAsTargetItem(e.target);
    }
  },
  redisplay: function() {
    var me = this;    
    var searchbox = $('q');     

    var ts = $('top-sites');
    while (ts.firstChild) { ts.removeChild(ts.firstChild); }    

    var content = $('history'); 
    while (content.firstChild) { content.removeChild(content.firstChild); }


    var viewedItems, topSites;
    var search = searchbox.value;
    if (search)
      search = search.strip();
    var searchIsWeblink = urlUtils.parseWebLink(search);
    if (search && search.length > 1) {
      $("search-info-bar").style.display = "block";
      this.renderSearchInfoBar(search, searchIsWeblink);
      topSites = this.journal.searchTopSites(search);
      viewedItems = this.journal.search(search, 6);
      if (viewedItems.length == 0) {
        content.appendChild(domUtils.createSpanText("(No results)", "no-results"))
      }
    } else {
      $("search-info-bar").style.display = "none";
      topSites = this.journal.searchTopSites(null);
      viewedItems = this.journal.getLastHistoryDay();
    }

    if (topSites && topSites.root.hasChildren) {
        this.appendTopSites(topSites);
    }

    if (viewedItems && viewedItems.root.hasChildren) {
      viewedItems.root.containerOpen = true;
      for (var i = 0; i < viewedItems.root.childCount; i++) {
        this.appendDaySet(viewedItems.root.getChild(i));
      }
      viewedItems.root.containerOpen = false;
    }

    this.sidebars.each(function (sb) {
      if (sb.searchInteractive)
        sb.searchInteractive(search);
    });

    var searchPrimary = $("search-primary");
    while (searchPrimary.firstChild) { searchPrimary.removeChild(searchPrimary.firstChild); };
    var searchSecondary = $("search-secondary");
    while (searchSecondary.firstChild) { searchSecondary.removeChild(searchSecondary.firstChild); };
    searchSecondary.style.display = "none";
    if (search) {
      // Now add the alternative search links
      var engines = SEARCH_SERVICE.getEngines(Object()); /* NS strongly desires an Out argument to be an object */
      var set = document.createElement("div");
      set.className = "set";

      if (searchIsWeblink) {
        var button = document.createElement("button");
        button.setAttribute("id", "search-provider");
        Event.observe(button, 'click', function() { window.location.href = searchIsWeblink; } );
        button.appendChild(document.createTextNode("Go To Website"));
        searchPrimary.appendChild(button);
      } else {
        var currentEngine = SEARCH_SERVICE.currentEngine;
        var button = document.createElement("button");
        button.setAttribute("id", "search-provider");
        Event.observe(button, 'click', function() { window.location.href = currentEngine.getSubmission(search, null).uri.spec; } );
        var img = document.createElement("img");
        img.className = "search-engine";
        img.src = currentEngine.iconURI.spec;
        button.appendChild(img);
        button.appendChild(document.createTextNode(" Search " + currentEngine.name));
      }
        var hint = domUtils.createSpanText(" (Ctrl-Enter)", "keybinding-hint");
        button.appendChild(hint);
        searchPrimary.appendChild(button);

      for (var i = 1; i < engines.length; i++) {
        var engine = engines[i];
        var div = document.createElement("div");
        var a = document.createElement("a");
        a.href = engine.getSubmission(search, null).uri.spec;
        a.appendChild(document.createTextNode("Search "));
        var img = document.createElement("img");
        img.className = "search-engine";
        img.src = engine.iconURI.spec;
        a.appendChild(img);
        a.appendChild(document.createTextNode(" " + engine.name));
        div.appendChild(a);
        searchSecondary.appendChild(div);
        a.setAttribute("id", "altsearch-" + i);
      }
    }
  },
  clearSearch : function() {
    var searchbox = $('q');
    searchbox.value='';
    this.searchValue = null;
    this.doSearch();
    this.doSearchQuery();
    searchbox.select();
    searchbox.focus();
  },
  handleWindowKeyUp: function(e) {

    // ESC or Ctrl-c is clear search
    if (e.keyCode == 27 || (e.ctrlKey && e.keyCode == 67)) {
      this.clearSearch();
      Event.stop(e);
      return;
    }

    // LOG("handling window KEYUP" + e + " " + e.keyCode + " " + e.ctrlKey);
    
    if (!e.ctrlKey && e.keyCode == 13) {
      me.onSubmit();
      return true;
    }

    if (!e.ctrlKey)
      return;
    
    // Handle control bindings for links
    var click = document.createEvent("MouseEvents");
    click.initEvent("click", "true", "true");
    var target;     
    if (e.keyCode == 13) { 
      target = $("search-provider");
    } else if (e.keyCode >= 49 && e.keyCode <= 57) {  // 1-9
      var idx = e.keyCode - 48;
      target = $("altsearch-" + idx);
    } else {
      return;
    }
    if (target) {
      target.focus();
      target.dispatchEvent(click);
      Event.stop(e);
      return true;
    }    
  },
  onload: function() {
    var me = this;  

    this.journal = getJournalInstance();

    var prefs = Cc["@mozilla.org/preferences-service;1"].
                  getService(Ci.nsIPrefBranch);
   
    window.addEventListener("keyup", function (e) { me.handleWindowKeyUp(e); }, false);    
    
    var searchbox = document.getElementById('q');
    var searchform = document.forms['qform'];
    
    searchbox.addEventListener("keyup", function (e) { me.handleSearchChanged(e) }, false);
    searchform.addEventListener("submit", function (e) { Event.stop(e); }, true);
    
    var histcount = document.forms['histcount']; 
    if (histcount) {
      $("histcountentry").value = prefs.getIntPref("browser.history_expire_days");
      histcount.addEventListener("submit", function (e) { me.onHistValueChanged(); Event.stop(e); }, true);
    }

    searchbox.focus();
    
    $("history").appendChild(document.createTextNode("Loading journal..."));
    window.setTimeout(function () { try { me.initializeSidebars(); } catch (e) { LOG("exception: " + e); }  }, 50);  
    window.setTimeout(function () { try { me.redisplay(); } catch (e) { LOG("exception: " + e); }  }, 150);    
  },
  initializeSidebars: function() {
    // This function is separated out so internal IFRAMEs don't pollute the browser history, c.f.:
    // http://codinginparadise.org/weblog/2005/08/ajax-tutorial-tale-of-two-iframes-or.html
    this.sidebars.each(function (sb) {
      var div = document.createElement("div");
      div.setAttribute("id", sb.sidebarId);
      div.className = "sidebar";
      var header = document.createElement("h4");
      header.className = "sidebar-header";
      header.appendChild(document.createTextNode(sb.sidebarTitle));
      div.appendChild(header);
      sb.setupDom(div);
      $("sidebars").appendChild(div);
    });
  },
  onHistValueChanged: function () {
    var val = $("histcountentry").value;
    var intVal = parseInt(val);
    if (!intVal)
      return;
    var prefs = Cc["@mozilla.org/preferences-service;1"].
                  getService(Ci.nsIPrefBranch);
    prefs.setIntPref("browser.history_expire_days", intVal);    
  },
  clearSearchTimeouts: function() {
    if (typeof this.searchTimeout == "number") {
      window.clearTimeout(this.searchTimeout);
      this.searchTimeout = null;
    }
    if (typeof this.webSearchTimeout == "number") {
      window.clearTimeout(this.webSearchTimeout);
      this.webSearchTimeout = null;
    }  
  },
  onsubmit: function() {
    this.clearSearchTimeouts();
    if (this.targetHistoryItem) {
      window.location.href = this.targetHistoryItem.uri;
    }
  },
  doSearch: function() {
    this.redisplay(); 
  },
  doSearchQuery: function() {
    var me = this;
    this.sidebars.each(function (sb) {
      if (sb.searchQuery)
        sb.searchQuery(me.searchValue);
    });
  },
  handleSearchChanged: function(e) {
    var q = e.target;
    var search = q.value.strip()
    if (search == this.searchValue)
      return;
    this.searchValue = search;
    var me = this;
    if (!this.searchTimeout) {
      this.searchTimeout = window.setTimeout(function () { me.searchTimeout = null; me.doSearch() }, 350);
    }
    // This second query happens when the user stops typing
    if (this.searchQueryTimeout) {
      window.clearTimeout(this.searchQueryTimeout);
    }
    this.searchQueryTimeout = window.setTimeout(function () { me.searchQueryTimeout = null; me.doSearchQuery() }, 700);
  },
  appendSidebar: function(sb) {
    // test
    this.sidebars.push(sb);
  },
}
var theJournalPage;
var getJournalPageInstance = function() {
  if (theJournalPage == null)
    theJournalPage = new JournalPage();
  return theJournalPage;
}

Event.observe(window, "load", getJournalPageInstance().onload.bind(getJournalPageInstance()), false);

