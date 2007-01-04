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
