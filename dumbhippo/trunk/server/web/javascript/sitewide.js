// usage: onKeyPress="return onEnterFalse(event)"
onEnterFalse = function(e) {
   return e.keyCode != 13;
}

trimWhitespace = function(src) {
   var s = src;
   s = s.replace(/^\s+/, "");
   s = s.replace(/\s+$/, "");
   return s;
}

isJustWhitespace = function(str) {
  var s = trimWhitespace(str);
  return s.length == 0;
}

function deleteChildren(node) {
   while (node.firstChild) {
       node.removeChild(node.firstChild);
   }
}

setError = function(nodeName, errorMessage) {
   var node = document.getElementById(nodeName);
   node.setAttribute("class", "validity-error");
   deleteChildren(node);
   node.appendChild(document.createTextNode(errorMessage));
}

function unsetError(nodeName) {
   var node = document.getElementById(nodeName);
   deleteChildren(node);
}

function loginRedirect(loginPage, goBackTo) {
	window.location.replace(loginPage + "?goBackTo=" + encodeURIComponent(goBackTo));
}
