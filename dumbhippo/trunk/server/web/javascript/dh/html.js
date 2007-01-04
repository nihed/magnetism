dojo.provide("dh.html");

// A subset of dojo.html

// RAR: this function comes from nwidgets and is more-or-less unmodified.
// We should probably look ant Burst and f(m)'s equivalents
dh.html.getAttribute = function (node, attr){
	// FIXME: need to add support for attr-specific accessors
	if((!node)||(!node.getAttribute)){
		// if(attr !== 'nwType'){
		//	alert("getAttr of '" + attr + "' with bad node"); 
		// }
		return null;
	}
	var ta = typeof attr == 'string' ? attr : new String(attr);

	// first try the approach most likely to succeed
	var v = node.getAttribute(ta.toUpperCase());
	if((v)&&(typeof v == 'string')&&(v!="")){ return v; }

	// try returning the attributes value, if we couldn't get it as a string
	if(v && typeof v == 'object' && v.value){ return v.value; }

	// this should work on Opera 7, but it's a little on the crashy side
	if((node.getAttributeNode)&&(node.getAttributeNode(ta))){
		return (node.getAttributeNode(ta)).value;
	}else if(node.getAttribute(ta)){
		return node.getAttribute(ta);
	}else if(node.getAttribute(ta.toLowerCase())){
		return node.getAttribute(ta.toLowerCase());
	}
	return null;
}
	
/**
 *	Determines whether or not the specified node carries a value for the
 *	attribute in question.
 */
dh.html.hasAttribute = function (node, attr){
	var v = dh.html.getAttribute(node, attr);
	return v ? true : false;
}

/**
 * Returns the string value of the list of CSS classes currently assigned
 * directly to the node in question. Returns an empty string if no class attribute
 * is found;
 */
dh.html.getClass = function (node) {
	if(node.className){
		return node.className;
	}else if(dh.html.hasAttribute(node, "class")){
		return dh.html.getAttribute(node, "class");
	}
	return "";
}

/**
 * Adds the specified class to the beginning of the class list on the
 * passed node. This gives the specified class the highest precidence
 * when style cascading is calculated for the node. Returns true or
 * false; indicating success or failure of the operation, respectively.
 */
dh.html.prependClass = function (node, classStr){
	if(!node){ return null; }
	if(dh.html.hasAttribute(node,"class")||node.className){
		classStr += " " + (node.className||dh.html.getAttribute(node, "class"));
	}
	return dh.html.setClass(node, classStr);
}

/**
 * Adds the specified class to the end of the class list on the
 *	passed &node;. Returns &true; or &false; indicating success or failure.
 */
dh.html.addClass = function (node, classStr){
	if (!node) { throw new Error("addClass: node does not exist"); }
	if(dh.html.hasAttribute(node,"class")||node.className){
		classStr = (node.className||dh.html.getAttribute(node, "class")) + " " + classStr;
	}
	return dh.html.setClass(node, classStr);
}

/**
 *  Clobbers the existing list of classes for the node, replacing it with
 *	the list given in the 2nd argument. Returns true or false
 *	indicating success or failure.
 */
dh.html.setClass = function (node, classStr){
	if(!node){ return false; }
	var cs = new String(classStr);
	try{
		if(typeof node.className == "string"){
			node.className = cs;
		}else if(node.setAttribute){
			node.setAttribute("class", classStr);
			node.className = cs;
		}else{
			return false;
		}
	}catch(e){
		dh.debug("__util__.setClass() failed", e);
	}
	return true;
}

/**
 * Removes the className from the node;. Returns
 * true or false indicating success or failure.
 */ 
dh.html.removeClass = function (node, classStr){
	if(!node){ return false; }
	var classStr = dh.string.trim(new String(classStr));

	try{
		var cs = String( node.className ).split(" ");
		var nca  = [];
		for(var i = 0; i<cs.length; i++){
			if(cs[i] != classStr){ 
				nca .push(cs[i]);
			}
		}
		node.className = nca .join(" ");
	}catch(e){
		dh.debug("__util__.removeClass() failed", e);
	}

	return true;
}

// Enum type for getElementsByClass classMatchType arg:
dh.html.classMatchType = {
	ContainsAll : 0, // all of the classes are part of the node's class (default)
	ContainsAny : 1, // any of the classes are part of the node's class
	IsOnly : 2 // only all of the classes are part of the node's class
}

/**
 * Returns an array of nodes for the given classStr, children of a
 * parent, and optionally of a certain nodeType.
 * NOT REALLY RECOMMENDED - this function is crazy inefficient and it's just 
 * gross. Let's try to get rid of usage of it.
 */
dh.html.getElementsByClass = function (classStr, parent, nodeType, classMatchType) {
	if(!parent){ parent = document; }
	var classes = classStr.split(/\s+/g);
	var nodes = [];
	if( classMatchType != 1 && classMatchType != 2 ) classMatchType = 0; // make it enum

	// FIXME: doesn't have correct parent support!
	if(false && document.evaluate) { // supports dom 3 xpath
		var xpath = "//" + (nodeType || "*") + "[contains(";
		if(classMatchType != dh.html.classMatchType.ContainsAny){
			xpath += "concat(' ',@class,' '), ' " +
				classes.join(" ') and contains(concat(' ',@class,' '), ' ") +
				" ')]";
		}else{
			xpath += "concat(' ',@class,' '), ' " +
				classes.join(" ')) or contains(concat(' ',@class,' '), ' ") +
				" ')]";
		}
		//dojo.debug("xpath: " + xpath);

		var xpathResult = document.evaluate(xpath, parent, null,
			XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE, null);

		outer:
		for(var node = null, i = 0; node = xpathResult.snapshotItem(i); i++){
			if(classMatchType != dh.html.classMatchType.IsOnly){
				nodes.push(node);
			}else{
				if(!dh.html.getClass(node)){ continue outer; }

				var nodeClasses = dh.html.getClass(node).split(/\s+/g);
				var reClass = new RegExp("(\\s|^)(" + classes.join(")|(") + ")(\\s|$)");
				for(var j = 0; j < nodeClasses.length; j++) {
					if( !nodeClasses[j].match(reClass) ) {
						continue outer;
					}
				}
				nodes.push(node);
			}
		}
	}else{
		if(!nodeType){ nodeType = "*"; }
		var candidateNodes = parent.getElementsByTagName(nodeType);

		outer:
		for(var i = 0; i < candidateNodes.length; i++) {
			var node = candidateNodes[i];
			if( !dh.html.getClass(node) ) { continue outer; }
			var nodeClasses = dh.html.getClass(node).split(/\s+/g);
			var reClass = new RegExp("(\\s|^)((" + classes.join(")|(") + "))(\\s|$)");
			var matches = 0;

			for(var j = 0; j < nodeClasses.length; j++) {
				if( reClass.test(nodeClasses[j]) ) {
					if( classMatchType == dh.html.classMatchType.ContainsAny ) {
						nodes.push(node);
						continue outer;
					} else {
						matches++;
					}
				} else {
					if( classMatchType == dh.html.classMatchType.IsOnly ) {
						continue outer;
					}
				}
			}

			if( matches == classes.length ) {
				if( classMatchType == dh.html.classMatchType.IsOnly && matches == nodeClasses.length ) {
					nodes.push(node);
				} else if( classMatchType == dh.html.classMatchType.ContainsAll ) {
					nodes.push(node);
				}
			}
		}
	}
	return nodes;
}
//this.getElementsByClassName = this.getElementsByClass;
