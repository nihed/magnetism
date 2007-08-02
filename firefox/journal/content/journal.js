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
 * Colin Walters.
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
 
const RDF = Components.classes["@mozilla.org/rdf/rdf-service;1"].getService(Components.interfaces.nsIRDFService); 
const BOOKMARK_NAME = RDF.GetResource("http://home.netscape.com/NC-rdf#Name");
const BOOKMARK_DATE = RDF.GetResource("http://home.netscape.com/NC-rdf#Date");
const BOOKMARK_VISITCOUNT = RDF.GetResource("http://home.netscape.com/NC-rdf#VisitCount");

function LOG(msg) {
  var consoleService = Components.classes["@mozilla.org/consoleservice;1"]
                                 .getService(Components.interfaces.nsIConsoleService);
  consoleService.logStringMessage(msg);}


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

var getHistory = function() {
    var gh = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIRDFDataSource);
    var iter = gh.GetAllResources();
    var result = [];
    while (iter.hasMoreElements()) {
      var item = iter.getNext();
      var resource = item.QueryInterface(Components.interfaces.nsIRDFResource);
      var itemname = readRDFString(gh, resource, BOOKMARK_NAME);
      var itemdate = readRDFDate(gh, resource, BOOKMARK_DATE);
      var itemcount = readRDFInt(gh, resource, BOOKMARK_VISITCOUNT);

      var action = "visited";
      if (resource.Value.startsWith("http://www.google.com/") && resource.Value.toQueryParams()["q"] ) { 
        /* detect google web searches, should be doing this in a better place */
        var googleSearchedFor = resource.Value.toQueryParams()["q"]
        itemname = googleSearchedFor.replace("+", " ");
        action = "searched for";
      }

      result.push({'name': itemname, 'date': itemdate, 'url': resource.Value, 'visitcount': itemcount, 'action' : action})
    }
    return result
}

function sliceByDay(histitems) {
  var days = {}
  for (var i = 0; i < histitems; i++) {
    var hi = histitems[i]
    var timeday = hi.date.getTime() / (24 * 60 * 60 * 1000)
    if (!days[timeday])
      days[timeday] = []
    days[timeday].push(hi)
  }
  LOG("pushing days: " + days)
  var sorted_days = []
  for (timeday in days) {
    sorted_days.push([timeday, days[timeday]])
  }
  sorted_days.sort(function (a,b) { if (a[0] == b[0]) { return 0; } else if (a[0] > b[0]) { return 1; } else { return -1; } })
  LOG("sorted days: "+sorted_days)
  days = []
  for (var i = 0; i < sorted_days; i++) {
    days.push(sorted_days[i][1])
  }
  return days;
}

var createSpanText = function(text, className) {
  var span = document.createElement('span')
  span.className = className
  span.appendChild(document.createTextNode(text))
  return span
}

var createAText = function(text, href, className) {
  var a = document.createElement('a');
  a.setAttribute('class' , className);
  a.setAttribute('href', href);
  a.appendChild(document.createTextNode(text));
  return a;
}

var filterHistoryItems = function(items, q) {
  var result = []
  for (var i = 0; i < items.length; i++) {
    item = items[i]
    if (item['url'].indexOf(q) >= 0 || item['name'].indexOf(q) >= 0) {
      result.push(item)
    }
  }
  return result;
}

var journal = {
  redisplay: function() {    LOG("redisplay");
    var content = document.getElementById('history'); 
    var searchbox = document.getElementById('q');     
    while (content.firstChild) { content.removeChild(content.firstChild) };
    
    var histitems = getHistory();
    
    var headernode = document.createElement('h4');
    headernode.className = 'date';
    headernode.appendChild(document.createTextNode('Today'));
    content.appendChild(headernode);
    var histnode = document.createElement('div');
    histnode.className = 'history';
    content.appendChild(histnode);
      
    var today_items = histitems; //sliceByDay(histitems)[0];
    if (searchbox.value) {
      content.appendChild(document.createTextNode("Searching for " + searchbox.value))
      today_items = filterHistoryItems(today_items, searchbox.value);
    }

    for (var i = 0; i < today_items.length; i++) {
      var histitemnode = document.createElement('div');
      histitemnode.className = 'item';
      var pad = function(x) { return x < 9 ? "0" + x : "" + x };
      var hrs = today_items[i].date.getHours();
      var mins = today_items[i].date.getMinutes();
      histitemnode.appendChild(createSpanText(pad(hrs) + ":" + pad(mins), 'time'));
      histitemnode.appendChild(createSpanText(today_items[i].action, 'action'));
      histitemnode.appendChild(createAText(today_items[i].name,today_items[i].url,'title'));

      var histmetanode = document.createElement('div');
      histmetanode.className = 'meta';
      histmetanode.appendChild(createSpanText(' ', 'blue'));
      histmetanode.appendChild(createSpanText(' ', 'tags'));
      histmetanode.appendChild(createAText(today_items[i].url.split("?")[0],today_items[i].url,'url'));
      histnode.appendChild(histitemnode);
      histnode.appendChild(histmetanode);
    }

  },
  onload: function() {
    this.searchTimeout = null;
    var searchbox = document.getElementById('q');
    var me = this;
    searchbox.onkeydown = function (e) { me.handleSearchChanged() } 
    this.redisplay();
    searchbox.focus()
  },
  idleDoSearch: function() {
    LOG("idle search");
    this.searchTimeout = null;
    this.redisplay(); 
  },
  handleSearchChanged: function() {
    if (!this.searchTimeout) {
      var me = this;
      this.searchTimeout = window.setTimeout(function () { me.idleDoSearch() }, 250);
      LOG("timeout id " + this.searchTimeout);
    }
  }
}
