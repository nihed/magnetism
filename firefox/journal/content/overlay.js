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

/* Match the junk we don't want so we can form decent URLs later.  If we don't want the URL at all, don't match it. */
var HistoryBlacklist = [ 
  new RegExp("(?:^https?:\/\/mail\.google\.com\/mail\/|^https?:\/\/mail\.google\.com\/a\/.+\/)(.+)"),
  new RegExp("(?:^https?:\/\/www\.google\.com\/calendar\/render|^https?:\/\/www\.google\.com\/calendar\/hosted\/.+\/render)(.+)"),
  new RegExp("^https?:\/\/www\.google\.com\/accounts\/.+"),
  new RegExp("^https?:\/\/www\.google\.com\/a\/.+\/ServiceLogin.+"),
  new RegExp("^https?:\/\/www\.google\.com\/reader\/view\/(.+)"),
  new RegExp("^http:\/\/www\.facebook\.com\/(home.php\??)"),
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
      if ( blacklistItem.test(uri.spec) )
        return i;
    }
    return -1;
  },
  possiblyHideUri: function(uri, time, sessid) {
    console.logStringMessage("possibly hiding URI: " + uri.spec);
    var rei = this.queryHideUri(uri);
    if ( rei >= 0 ) {
      try {
        this.browserHistory.removePage(uri);
      } catch (e) {
        console.logStringMessage("failed to hide page: " + e);
      }

      try {
        var junk = HistoryBlacklist[rei].exec(uri.spec)[1];
        if (junk) {
          uri.spec = uri.spec.replace(junk, "");
          console.logStringMessage("adding clean URI Visit: " + uri.spec);
          this.browserHistory.addVisit( uri, time, 0, this.browserHistory.TRANSITION_LINK, false, sessid );
        }
      } catch (e) {
        console.logStringMessage("failed to add clean uri: " + e);
      }
    }
  },
  onBeginUpdateBatch: function() { },
  onEndUpdateBatch: function() { },
  onVisit: function(uri, visitId, time, sessid, referid, transtype) {
    console.logStringMessage("visit: " + uri.spec);
    theHistoryMonkey.possiblyHideUri(uri, time, sessid);
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

