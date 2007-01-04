dojo.provide("dh.dom");

// This file is abridged from Dojo

dh.dom.ELEMENT_NODE                  = 1;
dh.dom.ATTRIBUTE_NODE                = 2;
dh.dom.TEXT_NODE                     = 3;
dh.dom.CDATA_SECTION_NODE            = 4;
dh.dom.ENTITY_REFERENCE_NODE         = 5;
dh.dom.ENTITY_NODE                   = 6;
dh.dom.PROCESSING_INSTRUCTION_NODE   = 7;
dh.dom.COMMENT_NODE                  = 8;
dh.dom.DOCUMENT_NODE                 = 9;
dh.dom.DOCUMENT_TYPE_NODE            = 10;
dh.dom.DOCUMENT_FRAGMENT_NODE        = 11;
dh.dom.NOTATION_NODE                 = 12;

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
