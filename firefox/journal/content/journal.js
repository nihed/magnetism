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
 
var getHistory = function() {
    var gh = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIRDFDataSource);
    iter = gh.GetAllResources();
    var result = [];
    while (iter.hasMoreElements()) {
      var item = iter.getNext();
      var resource = item.QueryInterface(Components.interfaces.nsIRDFResource);
      result.push(resource.Value)
    }
    return result
}

var createSpanText = function(text, className) {
  var span = document.createElement('span')
  span.className = className
  span.appendChild(document.createTextNode(text))
  return span
}
 
var journal = {
  onload: function() {
    document.getElementById('q').focus();

    var content = document.getElementById('content');
    var histitems = getHistory();
    
    var headernode = document.createElement('h4')
    headernode.className = 'date'
    headernode.appendChild(document.createTextNode('Today'))
    content.appendChild(headernode)
    var histnode = document.createElement('div')
    histnode.className = 'history'
    content.appendChild(histnode)
    
    for (var i = 0; i < histitems.length; i++) {
      var histitemnode = document.createElement('div')
      histitemnode.className = 'item'
      histitemnode.appendChild(createSpanText('00:00pm'))
      histitemnode.appendChild(createSpanText('visited', 'action'))
      var a = document.createElement('a')
      a.className = 'title'
      a.setAttribute('href', histitems[i])
      a.appendChild(document.createTextNode(histitems[i]))
      histitemnode.appendChild(a)
      
      histnode.appendChild(histitemnode)
    }
  }
 }
 