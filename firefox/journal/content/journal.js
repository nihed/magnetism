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

const JOURNAL_CHROME = "about:journal";

const BLANK_FAVICON = "chrome://mozapps/skin/places/defaultFavicon.png"

const DAY_MS = 24 * 60 * 60 * 1000; // a day

function LOG(msg) {
  var dl = $("debuglog");
  dl.appendChild(document.createTextNode(msg));
  dl.appendChild(document.createElement("br"));
}

/***** The Journal *****/

var JournalEntry = Class.create();
JournalEntry.prototype = {
  initialize: function(histitem) {
    this.histitem = histitem;

    this.url = histitem.url;
    this.date = histitem.lastVisitDate;
    this.displayUrl = formatUtils.ellipsize(histitem.url.split("?")[0], 50);
    this.title = this.histitem.title;
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
    lastHistoryItemResults.root.containerOpen = true;
    if (!lastHistoryItemResults.root.hasChildren) {
      lastHistoryItemResults.root.containerOpen = false;
      return null;
    }
    var lastHistoryItem = lastHistoryItemResults.root.getChild(0);
    var lastHistoryTime = lastHistoryItem.time;
    lastHistoryItemResults.root.containerOpen = false;

    histq = HISTORY_SERVICE.getNewQuery();
    options = this._getBaseQueryOptions();
    histq.beginTimeReference = histq.TIME_RELATIVE_EPOCH;
    // time is microseconds, not milliseconds
    histq.beginTime = lastHistoryTime  - (DAY_MS * 1000);
    histq.endTimeReference = histq.TIME_RELATIVE_EPOCH;
    histq.endTime = lastHistoryTime;

    return HISTORY_SERVICE.executeQuery(histq, options);
  },
  search: function(q, limit) {
    var options = this._getBaseQueryOptions();
    var histq = HISTORY_SERVICE.getNewQuery();
    histq.searchTerms = q;
    // FIXME - uncomment this when https://bugzilla.mozilla.org/show_bug.cgi?id=394508 is in
    // options.maxResults = limit;
    return HISTORY_SERVICE.executeQuery(histq, options);
  },
  searchTopSites: function(q) {
    var options = this._getBaseQueryOptions();
    options.setGroupingMode([], 0);
    options.sortingMode = options.SORT_BY_VISITCOUNT_DESCENDING;
    var histq = HISTORY_SERVICE.getNewQuery();
    if (q) histq.searchTerms = q;
    /* FIXME - picking a static value for this is a little silly */
    histq.minVisits = 3; 
    // FIXME - uncomment this when https://bugzilla.mozilla.org/show_bug.cgi?id=394508 is in
    // options.maxResults = 5;
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
  },
  appendDaySet: function(dayset) {
    dayset.QueryInterface(Ci.nsINavHistoryContainerResultNode);
    dayset.containerOpen = true;
    var date = new Date(dayset.getChild(0).time/1000);
    var today = new Date();

    var content = $('history');    
    var headernode = document.createElement('h4');
    headernode.setAttribute('class' , 'date');
    if (dateTimeUtils.getLocalDayOffset(today) == dateTimeUtils.getLocalDayOffset(date))
      headernode.appendChild(document.createTextNode("Today"))
    else
      headernode.appendChild(document.createTextNode(formatUtils.monthName(date.getMonth()) + " " + formatUtils.pad(date.getDate()) + " " + date.getFullYear()));
    content.appendChild(headernode);
    var histnode = document.createElement('div');
    histnode.setAttribute('class', 'set');
    content.appendChild(histnode);

    for (var i = 0; i < dayset.childCount; i++) {
      histnode.appendChild(this.renderJournalItem(dayset.getChild(i), false));
    }
    dayset.containerOpen = false;
  },
  appendTopSites: function(siteset) {

    siteset.root.containerOpen = true;
    var count = (siteset.root.childCount > 5)? 5 : siteset.root.childCount;
    if (count > 0) {

      var content = $('top-sites');    
      var headernode = document.createElement('h4');
      headernode.appendChild(document.createTextNode("Top Sites"));
      content.appendChild(headernode);
      var sitenode = document.createElement('div');
      sitenode.setAttribute('class', 'set');

      for (var i = 0; i < count; i++) {
        sitenode.appendChild(this.renderJournalItem(siteset.root.getChild(i), true));
      }

      content.appendChild(sitenode);
    }

    siteset.root.containerOpen = false;

  },
  renderJournalItemContent: function(entry, item) {
    var iconSection = document.createElement('div');
    iconSection.setAttribute('class', 'favicon');
    var a = document.createElement('a');
    a.href = entry.uri;
    iconSection.appendChild(a);
    var img = document.createElement('img');
    img.setAttribute('class', 'favicon-img');
    img.setAttribute('src', (entry.icon)? entry.icon.spec : BLANK_FAVICON);
    a.appendChild(img);
    item.appendChild(iconSection);     

    var urlSection = document.createElement('div');
    urlSection.setAttribute('class', 'urls');
    var titleDiv = document.createElement('div');
    titleDiv.appendChild(domUtils.createSpanText(entry.title,'title'));
    urlSection.appendChild(titleDiv);
    var hrefDiv = document.createElement('div');
    hrefDiv.appendChild(domUtils.createSpanText(formatUtils.ellipsize(entry.uri, 80), 'url'));
    urlSection.appendChild(hrefDiv);
    item.appendChild(urlSection);
  },
  renderJournalItem: function(entry, top) {
    var me = this;  

    var item = document.createElement('a');
    item.href = entry.uri;
    item.setAttribute('class', 'item');
    item.setAttribute('tabindex', 1); 

    item.addEventListener("focus", function(e) { me.onResultFocus(e, true); }, false);
    item.addEventListener("blur", function(e) { me.onResultFocus(e, false); }, false);             
    
    if (entry.time && !top) {
      var dateTime = new Date(entry.time/1000);
      var timeText = formatUtils.twelveHour(dateTime.getHours()) + ":" + formatUtils.pad(dateTime.getMinutes()) + " " + formatUtils.meridiem(dateTime.getHours());
      item.appendChild(domUtils.createSpanText(timeText, 'time'));
    } 
    
    this.renderJournalItemContent(entry, item);

    return item;
  },
  setAsTargetItem: function (node) {
    node.addClassName("target-item");
  },
  unsetAsTargetItem: function (node) {
    node.removeClassName("target-item");
  },
  onResultFocus: function(e, focused) {
    if (focused) {
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
      topSites = this.journal.searchTopSites(search);
      viewedItems = this.journal.search(search, 6);
    } else {
      topSites = this.journal.searchTopSites(null);
      viewedItems = this.journal.getLastHistoryDay();
    }

    if (topSites && topSites.root.hasChildren) {
        this.appendTopSites(topSites);
    }

    if (viewedItems && viewedItems.root.hasChildren) {
      viewedItems.root.containerOpen = true;
      if (viewedItems.root.childCount == 0 && search) {
        content.appendChild(domUtils.createSpanText("\"The world is a book, those who do not travel read only one page.\" ~ St. Augustine", "no-results"))
      } else {
        for (var i = 0; i < viewedItems.root.childCount; i++) {
          this.appendDaySet(viewedItems.root.getChild(i));
        }
      }
      viewedItems.root.containerOpen = false;
    }

    this.sidebars.each(function (sb) {
      if (sb.searchInteractive)
        sb.searchInteractive(search);
    });

    var searchPrimary = $("search-primary");
    while (searchPrimary.firstChild) { searchPrimary.removeChild(searchPrimary.firstChild); };

    if (search) {
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
        var hint = domUtils.createSpanText(" [Ctrl-Enter]", "keybinding-hint");
        button.appendChild(hint);
        searchPrimary.appendChild(button);
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
    var me = this;

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
  onSubmit: function() {
    this.clearSearchTimeouts();
  },
  doSearch: function() {
    this.redisplay(); 
  },
  doSearchQuery: function() {
    this.sidebars.each(function (sb) {
      if (sb.searchQuery)
        sb.searchQuery($('q').value);
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

