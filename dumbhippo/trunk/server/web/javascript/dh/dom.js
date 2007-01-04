dojo.provide("dh.dom");

// This file is from Dojo

dh.dom.removeChildren = function (node) {
	var count = node.childNodes.length;
	while(node.hasChildNodes()) { node.removeChild(node.firstChild); }
	return count;
}

dh.dom.replaceChildren = function (node, newChild) {
	dh.dom.removeChildren(node);
	node.appendChild(newChild);
}

dh.dom.textContent = function (node, text) {
	if (text) {
		dh.dom.replaceChildren(node, document.createTextNode(text));
		return text;
	} else {
		var _result = "";
		if (node == null) { return _result; }
		for (var i = 0; i < node.childNodes.length; i++) {
			switch (node.childNodes[i].nodeType) {
				case 1: // ELEMENT_NODE
				case 5: // ENTITY_REFERENCE_NODE
					_result += dh.dom.textContent(node.childNodes[i]);
					break;
				case 3: // TEXT_NODE
				case 2: // ATTRIBUTE_NODE
				case 4: // CDATA_SECTION_NODE
					_result += node.childNodes[i].nodeValue;
					break;
				default:
					break;
			}
		}
		return _result;
	}
}
