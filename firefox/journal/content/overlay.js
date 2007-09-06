/* ***** BEGIN LICENSE BLOCK *****
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

const console = Components.classes["@mozilla.org/consoleservice;1"].getService(Ci.nsIConsoleService);

var HistoryBlacklist = [ /* Match what we don't want to see, showing only what we would.  would be nice to use literals, but they don't play well inside an array */
  new RegExp("^https?:\/\/mail\.google\.com\/(mail|\/.+|a\/.+\/.+)"),
  new RegExp("^https?:\/\/www\.google\.com\/calendar\/(render\/.+|hosted\/.+/render\/.+)"),
  new RegExp("^https?:\/\/www\.google\.com\/accounts\/.+"),
  new RegExp("^javascript:.+"),
];

var theHistoryMonkey;
var HistoryMonkey = function() {
  this.navHistoryService = Components.classes["@mozilla.org/browser/nav-history-service;1"].getService(Components.interfaces.nsINavHistoryService);
  this.browserHistory = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIBrowserHistory);
  this.navHistoryService.addObserver(this, false);
  theHistoryMonkey = this;
};
HistoryMonkey.prototype = {
  queryHideUri: function(uri) {
    console.logStringMessage("evaluating: " + uri.spec);
    for (var i = 0; i < HistoryBlacklist.length; i++) {
      var blacklistItem = HistoryBlacklist[i];
      console.logStringMessage("using regex: " + blacklistItem);
      if ( blacklistItem.test(uri.spec) )
        return true;
    }
    return false;
  },
  possiblyHideUri: function(uri) {
    if (this.queryHideUri(uri)) {
      console.logStringMessage("hiding: " + uri.spec);
      try {
        /* 
         *  It might be worthwhile to try inserting the version of the URI that is ok, in case we don't have it already... 
         *  If we used the () match facility of the regex properly we could get the objects back needed to strip the URL of what we don't want
         */
        this.browserHistory.removePage(uri); 
      } catch (e) {
        console.logStringMessage("failed to hide page: " + e);
      }
    }
  },
  onBeginUpdateBatch: function() { },
  onEndUpdateBatch: function() { },
  onVisit: function(uri, visitId, time, sessid, referid, transtype) {
    console.logStringMessage("visit: " + uri.spec);
    theHistoryMonkey.possiblyHideUri(uri);
/*  
    var feeds = gBrowser.selectedBrowser.feeds;  //   gBrowser.mCurrentBrowser.feeds;
    for ( var i = 0; i < feeds.length; i++ ) {
      var title = feeds[i].title;
      var href = feeds[i].href;
    }
*/
  },
  onTitleChanged: function(uri, title) { console.logStringMessage("onTitleChanged" + uri.prePath + " : " + title); },
  onDeleteURI: function() { } ,
  onClearHistory: function() { },
  onPageChanged: function(uri, what, val) { },
  onPageExpired: function(uri, visitTime, wholeEntry) { },
};

var firefoxjournal = {
  onLoad: function() {
    console.logStringMessage("initializing");
    // initialization code
    this.initialized = true;
    // Do first time bits
    this.firstTimeInit();        
    this.strings = document.getElementById("firefoxjournal-strings");
    var container = gBrowser.tabContainer;
    var me = this;
    container.addEventListener("TabOpen", function(e) { me.onTabOpen(e); }, false);
    console.logStringMessage("creating history monkey");
    theHistoryMonkey = new HistoryMonkey();   
    console.logStringMessage("journal initialized");
  },
  onTabOpen: function(e) {
    var browser = e.target.linkedBrowser;
    if (!browser.currentURI)
      browser.loadURI(JOURNAL_CHROME);
  },
  getJournalPrefs : function() {
  	if (!this.branch) {
      var manager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefService);
      this.branch = manager.getBranch("extensions.firefoxjournal@redhat.com.");
    }
    return this.branch;    
  },
  firstTimeInit : function() {
    var jprefs = this.getJournalPrefs();
    if (jprefs.getBoolPref("firstTime")) {
      return;
    }  
    
    var prefs = Components.classes["@mozilla.org/preferences-service;1"].
                  getService(Components.interfaces.nsIPrefBranch);    
    var oldHomepage = prefs.getCharPref("browser.startup.homepage");
    jprefs.setCharPref("oldHomepage", oldHomepage);
    prefs.setCharPref("browser.startup.homepage", JOURNAL_CHROME);
    prefs.setIntPref("browser.startup.page", 1);

    jprefs.setBoolPref("firstTime", true);     
  },
};
window.addEventListener("load", function(e) { firefoxjournal.onLoad(e); }, false);

