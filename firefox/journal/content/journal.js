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
 
const JOURNAL_CHROME = "chrome://firefoxjournal/content/journal.html"; 

const DAY_MS = 24 * 60 * 60 * 1000;

function LOG(msg) {
  var dl = $("debuglog");
  dl.appendChild(document.createTextNode(msg));
  dl.appendChild(document.createElement("br"));
}

/***** History wrapper API *****/

var HistoryItem = Class.create();
HistoryItem.prototype = {
  initialize: function(url, title, lastVisitDate, visitCount) {
    this.url = url;
    this.title = title;
    this.lastVisitDate = lastVisitDate;
    this.visitCount = visitCount;
  }
}
 
const RDF = Components.classes["@mozilla.org/rdf/rdf-service;1"].getService(Components.interfaces.nsIRDFService); 
const BOOKMARK_NAME = RDF.GetResource("http://home.netscape.com/NC-rdf#Name");
const BOOKMARK_DATE = RDF.GetResource("http://home.netscape.com/NC-rdf#Date");
const BOOKMARK_VISITCOUNT = RDF.GetResource("http://home.netscape.com/NC-rdf#VisitCount");

function readRDFThingy(ds,res,prop,qi,def) {
  var val = ds.GetTarget(res, prop, true);
  if (val)
    return val.QueryInterface(qi).Value;
  else
    return def;
}

function readRDFString(ds,res,prop) {
  return readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFLiteral,"")
}

function readRDFDate(ds,res,prop) {
  return new Date(readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFDate,null)/1000);
}

function readRDFInt(ds,res,prop) {
  return readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFInt,-1);
}

var History = Class.create();
Object.extend(History.prototype, Enumerable);

Object.extend(History.prototype, {
  initialize: function() {
    this.history = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIRDFDataSource);
    this.cachedHistoryCount = -1;
    this.observers = [];
    this.timeout = null;
  },
  _each: function(iterator) {
    var historyRdf = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIRDFDataSource);
    var iter = this.history.GetAllResources();
    var result = [];
    while (iter.hasMoreElements()) {
      var resource = iter.getNext().QueryInterface(Components.interfaces.nsIRDFResource);
      var histItem = new HistoryItem(resource.Value, 
                                     readRDFString(this.history, resource, BOOKMARK_NAME),
                                     readRDFDate(this.history, resource, BOOKMARK_DATE),
                                     readRDFInt(this.history, resource, BOOKMARK_VISITCOUNT));
      iterator(histItem);
    }
  },
  observeChange: function(observer) {
    this.observers.push(observer);
    if (!this.timeout) {
      var me = this;
      this.timeout = new PeriodicalExecuter(function(pe) { me.checkChanged(); }, 5);
    }
  },
  checkChanged: function(pe) {
    if (!this.observers) {
      pe.stop();  
      return;
    }
    if (this.cachedHistoryCount != this.history.count) {
      this.cachedHistoryCount = this.history.count;
      var me = this;
      this.observers.each(function (it) { it(me); });
    }
  }
});

var theHistory;
var getHistoryInstance = function() {
  if (theHistory == null)
    theHistory = new History();
  return theHistory;
}

/***** The Journal *****/

var JournalEntry = Class.create();
JournalEntry.prototype = {
  initialize: function(histitem) {
    this.histitem = histitem;

    this.url = histitem.url;
    this.date = histitem.lastVisitDate;
    this.displayUrl = histitem.url.split("?")[0].substring(0,50) + ((histitem.url.split("?")[0].length > 50)? "..." : "");
    this.title = this.histitem.title;
    this.actionIcon = null;
    this.action = 'visited';
    
    var queryParams = histitem.url.toQueryParams();
    
    if (histitem.url.startsWith("http://www.google.com/") && queryParams["q"]) { 
      /* detect google web searches */
      this.title = decodeURIComponent(queryParams["q"].replace(/\+/g," "));
      this.action = "googled for";
    }
    else if (histitem.url.startsWith("http://search.yahoo.com/search") && queryParams["p"]) { 
      /* detect yahoo product searches */
      this.title = decodeURIComponent(queryParams["p"].replace(/\+/g," "));
      this.action = "yahoo'd for";
      this.displayUrl =  "http://search.yahoo.com/" + this.title;
    }
    else if (histitem.url.startsWith("http://search.creativecommons.org/") && queryParams["q"]) { 
      /* detect cc product searches */
      this.title = decodeURIComponent(queryParams["q"].replace(/\+/g," "));
      this.action = "cc'd for";
      this.displayUrl =  "http://search.creativecommons.org/" + this.title;
    }
    else if (histitem.url.startsWith("http://www.amazon.com/") && queryParams["field-keywords"]) { 
      /* detect amazon product searches */
      this.title = decodeURIComponent(queryParams["field-keywords"].replace(/\+/g," "));
      this.action = "amazon'd for";
      this.displayUrl =  "http://www.amazon.com/" + this.title;
    }
    else if (histitem.url.startsWith("http://search.ebay.com/") && (queryParams["satitle"] || queryParams["query"] ) ) { 
      /* detect ebay product searches */
      /* FIXME: this doesn't always detect searches even from our own engine... */
      var q = queryParams["satitle"] || queryParams["query"];
      this.title = decodeURIComponent(q.replace(/\+/g," "));
      this.action = "ebay'd for";
      this.displayUrl =  "http://www.ebay.com/" + this.title;
    }    
  },
  matches: function (q) {
    return this['url'].indexOf(q) >= 0 || this['title'].toLowerCase().indexOf(q) >= 0;
  },
} 

var getLocalDayOffset = function(date, tzoffset) {
  var tzoff = tzoffset || (new Date().getTimezoneOffset() * 60 * 1000);
  return Math.floor((date.getTime() - tzoff) / DAY_MS)  
}

var Journal = Class.create();
Journal.prototype = {
  initialize: function () {
    this.history = getHistoryInstance();
    var me = this;
    this.history.observeChange(function () { me.onHistoryChange(); });
    this.journalEntries = null;
    this.onHistoryChange();
  },
  onHistoryChange: function() {
    var me = this;
    var tzoffset = (new Date().getTimezoneOffset() * 60 * 1000);
    
    // Generate a hash mapping day offset to set of journal entries
    var days = $H();
    this.history.each(function (hi) {
      var timeday = getLocalDayOffset(hi.lastVisitDate, tzoffset);
      if (!days[timeday])
        days[timeday] = [];
      days[timeday].push(new JournalEntry(hi));
    });
        
    // Now sort those day offsets
    this.journalEntries = days.entries();
    this.journalEntries.sort(function (a,b) { if (a[0] < b[0]) { return 1; } else { return -1; } });
    
    // Strip the day offset, finally generating an ordered list of journal entries grouped by day
    for (var i = 0; i < this.journalEntries.length; i++) {
      var tmpGroup = this.journalEntries[i];
      var entrySet = tmpGroup[1];
      entrySet.sort(function (a,b) { if (a.date > b.date) { return -1; } else { return 1; }});
      this.journalEntries[i] = entrySet;
    }
  },
  search: function (q, limit, it) {
  	var count = 0;  
    q = q.toLowerCase();
    this.journalEntries.each(function (entrySet) {
  	  var space = limit - count;
	    if (space == 0)
	      throw $break;    
      var filteredSet = [];    
      entrySet.each(function (entry) {
        if (entry.matches(q)) {
          filteredSet.push(entry);
          count = count+1;
          if (limit - count == 0)
            throw $break;
        }
      });
      if (filteredSet.length > 0)
        it(filteredSet);
    });
  },
}

var theJournal;
var getJournalInstance = function() {
  if (theJournal == null)
    theJournal = new Journal();
  return theJournal;
}
 
var findHighestVisited = function(journalEntries) {
  var highest;
  journalEntries.each(function (entryList) {
    entryList.each(function (histitem) {
      if (!highest) {
        highest = histitem;
      } else if (highest.visitcount < histitem.visitcount) {
        highest = histitem;
      }
    });
  });
  return highest;
}

var isWebLink = function(text) {
  var idx = text.lastIndexOf('.')
  if (idx > 0) {
    return $A(["com", "org", "net", "uk", "us", "cn", "fm"]).include(text.substring(idx+1))
  }
  return false;
}

var parseUserUrl = function(text) {
  if (text.match(/^[A-Za-z]:.*$/)) {
    return text;
  }
  return "http://" + text;
}

var createSpanText = function(text, className) {
  var span = document.createElement('span')
  if (className) span.className = className
  span.appendChild(document.createTextNode(text))
  return span
}

var createAText = function(text, href, className) {
  var a = document.createElement('a');
  if (className) a.setAttribute('class' , className);
  a.setAttribute('href', href);
  a.appendChild(document.createTextNode(text));
  return a;
}

var pad = function(x) { return x < 10 ? "0" + x : "" + x };
var twelveHour = function(x) { return (x > 12) ? (x % 12) : x };
var meridiem = function(x) { return (x > 12) ? "pm" : "am" };

var formatMonth = function(i) { return ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"][i]}

var JournalPage = {
  appendDaySet: function(dayset) {
    var date = dayset[0].date;
    var today = new Date();

    var content = $('history');    
    var headernode = document.createElement('h4');
    headernode.className = 'date';
    if (getLocalDayOffset(today) == getLocalDayOffset(date))
      headernode.appendChild(document.createTextNode("Today, "))
    headernode.appendChild(document.createTextNode(formatMonth(date.getMonth()) + " " + pad(date.getDate()) + " " + date.getFullYear()));
    content.appendChild(headernode);
    var histnode = document.createElement('div');
    histnode.className = 'set';
    content.appendChild(histnode);

    for (var i = 0; i < dayset.length; i++) {
      histnode.appendChild(this.renderJournalItem(dayset[i]));
    }
  },
  renderJournalItem: function(entry) {
    var me = this;  
    var isTarget = entry == this.targetHistoryItem;

    var item = document.createElement('a');
    item.href = entry.url;
    item.className = 'item';
    item.setAttribute('tabindex', 1); 

    if (isTarget) {
      this.setAsTargetItem(item);
      item.setAttribute('id', 'default-target-item');
    }

    item.addEventListener("focus", function(e) { me.onResultFocus(e, true); }, false);
    item.addEventListener("blur", function(e) { me.onResultFocus(e, false); }, false);             
    
    var timeText;
    if (entry.date)
      timeText = twelveHour(entry.date.getHours()) + ":" + pad(entry.date.getMinutes()) + " " + meridiem(entry.date.getHours());
    else
      timeText = ' '.times(15);
    item.appendChild(createSpanText(timeText, 'time'));
    item.appendChild(createSpanText(entry.action, 'action'));

    var urlSection = document.createElement('div');
    urlSection.className = 'urls';
    var titleDiv = document.createElement('div');
    titleDiv.appendChild(createSpanText(entry.title,'title'));
    urlSection.appendChild(titleDiv);
    var hrefDiv = document.createElement('div');
    hrefDiv.appendChild(createSpanText(entry.displayUrl,'url'));
    urlSection.appendChild(hrefDiv);
    item.appendChild(urlSection);

    return item;
  },
  searchInfoBar: function(q) {
      var me = this;
      var node = document.createElement("div");
      node.className = "search-info-bar";
 
      node.appendChild(createSpanText("Searching for ", "search-pre-term"));
      node.appendChild(createSpanText(q,"search-term"));
      var clearSearch = document.createElement("a");
      clearSearch.addEventListener("click", function() { me.clearSearch(); }, false);
      clearSearch.href = "javascript:void(0);";
      clearSearch.className = "clear-search";
      clearSearch.setAttribute("accesskey", "c");
      clearSearch.setAttribute("title", "Clear this search [shift-alt-c]");
      clearSearch.appendChild(document.createTextNode("[clear]"));
      node.appendChild(clearSearch);
      
      var searchService = Components.classes["@mozilla.org/browser/search-service;1"].getService(Components.interfaces.nsIBrowserSearchService);
      var currentEngine = searchService.currentEngine;
      if (currentEngine && currentEngine.name) {
        var searchUri = currentEngine.getSubmission(q, null).uri.spec;
        node.appendChild(document.createTextNode(" | "));  
        var searchNode = document.createElement("span");     
        searchNode.addClassName("search-provider");
        var a = document.createElement("a");
        a.setAttribute("href", searchUri);
        var img = document.createElement("img");
        img.setAttribute("src", currentEngine.iconURI.spec);
        a.appendChild(img);
        searchNode.appendChild(a);
        searchNode.appendChild(document.createTextNode(" "));
        a = document.createElement("a");
        a.setAttribute("id", "search-provider");        
        a.setAttribute("href", searchUri);   
        a.appendChild(document.createTextNode(currentEngine.name + ": " + q));
        searchNode.appendChild(a);
        searchNode.appendChild(createSpanText(" (Ctrl-Enter)", "keybinding-hint"));
        node.appendChild(searchNode);
      }
      return node;
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
    var content = $('history'); 
    var searchbox = $('q');     
    while (content.firstChild) { content.removeChild(content.firstChild); }
    
    var viewedItems;
    var search = searchbox.value;
    if (search)
      search = search.strip()
    if (search) {
      content.appendChild(this.searchInfoBar(search))

      viewedItems = []
      this.journal.search(search, 6, function (entrySet) { viewedItems.push(entrySet); });
      this.targetHistoryItem = findHighestVisited(viewedItems);
      if (viewedItems.length == 0) {
        content.appendChild(createSpanText("(No results)", "no-results"))
      }
    } else {
      viewedItems = this.journal.journalEntries;
      for (var i = 0; i < viewedItems.length; i++) {
        if (viewedItems[i].length > 0) {
          viewedItems = [viewedItems[i]]
          break;
        }
      }
    }

    for (var i = 0; i < viewedItems.length; i++) {
      this.appendDaySet(viewedItems[i]);
    }

    if (search) {    
      // Now add the alternative search links
      var searchService = Components.classes["@mozilla.org/browser/search-service;1"].getService(Components.interfaces.nsIBrowserSearchService);
      var engines = searchService.getEngines(Object()); /* NS strongly desires an Out argument to be an object */
      var set = document.createElement("div");
      set.className = "set";
      // Skip first engine, we displayed that above
      for (var i = 1; i < engines.length; i++) {
        var engine = engines[i];
        var linkItem = this.renderJournalItem({'title': engine.name,
                                               'url': engine.getSubmission(search, null).uri.spec,
                                               'displayUrl': engine.description,
                                               'action': "[ctrl-" + i + "]"
                                             });
        linkItem.setAttribute("id", "altsearch-" + i);
        set.appendChild(linkItem);
      }
      var altSearchH4 = document.createElement("h4");
      altSearchH4.appendChild(document.createTextNode("Alternative Searches"));
      $("history").appendChild(altSearchH4);
      $("history").appendChild(set);
    }
  },
  clearSearch : function() {
    var searchbox = $('q');
    searchbox.value='';
    this.redisplay();
    searchbox.select();
    searchbox.focus();
  },
  handleWindowKey: function(e) {
    if (!e.ctrlKey)
      return;
    
    // Ctrl-c is clear search
    if (e.keyCode == 67) {
      this.clearSearch();
      Event.stop(e);
      return;
    }
    
    // Handle control bindings for links
    var click = document.createEvent("MouseEvents");
    click.initEvent("click", "true", "true");
    var target;     
    if (e.keyCode == 13) { 
      target = $("search-provider");
    } else if (e.keyCode >= 49 && e.keyCode < 57) {  // 1-9
      var idx = e.keyCode - 49;
      target = $("altsearch-" + idx);
    } else {
      return;
    }
    if (target) {
      target.focus();
      target.dispatchEvent(click);
      Event.stop(e);
    }    
  },
  onload: function() {
    LOG("onload");
    var me = this;  
    this.searchTimeout = null;
    this.searchValue = null;
    this.targetHistoryItem = null;
    this.journal = getJournalInstance();
    
    window.addEventListener("keyup", function (e) { me.handleWindowKey(e); }, false);    
    
    var searchbox = document.getElementById('q');
    var searchform = document.forms['qform'];
    
    searchbox.addEventListener("keyup", function (e) { me.handleSearchChanged(e) }, false);
    searchform.addEventListener("submit", function (e) { me.onsubmit(); Event.stop(e); }, true);
    
    searchbox.focus();
    
    $("history").appendChild(document.createTextNode("Loading journal..."))
    window.setTimeout(function () { me.redisplay(); }, 150);    
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
      window.location.href = this.targetHistoryItem.url;
    }
  },
  idleDoSearch: function() {
    this.searchTimeout = null;
    this.redisplay(); 
  },
  idleDoWebSearch: function() {
    this.webSearchTimeout = null;
    var q = document.getElementById("q").value;
    if (q) {
      document.getElementById("google-q").src = "http://www.gnome.org/~clarkbw/google/?q=" + escape(q.strip());
    }    
  },
  handleSearchChanged: function(e) {
    var q = e.target;
    var search = q.value.strip()
    if (search == this.searchValue)
      return;
    this.searchValue = search;
    if (!this.searchTimeout) {
      var me = this;
      this.searchTimeout = window.setTimeout(function () { me.idleDoSearch() }, 350);
      this.webSearchTimeout = window.setTimeout(function () { me.idleDoWebSearch() }, 600);      
    }
  }
}

Event.observe(window, "load", JournalPage.onload.bind(JournalPage), false);

