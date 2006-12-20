dojo.provide('dh.stacker');
dojo.require('dh.util');

dh.stacker.removePrelight = function(node) {
	dh.util.removeClass(node, "dh-box-prelighted")
}

dh.stacker.blockOpen = function(block) {
	block.dhExpanded = true;
	var content = document.getElementById("dhStackerBlockContent-" + block.dhBlockId);
	if (content)
		content.style.display = "block";
	var controls = document.getElementById("dhStackerBlockControls-" + block.dhBlockId);
	if (controls)
		controls.style.display = "block";
	var qual = document.getElementById("dhStackerBlockTimeAgoQualifier-" + block.dhBlockId);
	if (qual)
		qual.style.display = "inline";		
	var fullDesc = document.getElementById("dhStackerBlockDescription-" + block.dhBlockId);
	var shortDesc = document.getElementById("dhStackerBlockHeaderDescription-" + block.dhBlockId);
	if (shortDesc) {
		fullDesc.style.display = "block";
		shortDesc.style.display = "none";
	}
	var message = document.getElementById("dhMusicBlockMessage-" + block.dhBlockId);
	if (message)
		message.style.display = "none";
}

dh.stacker.blockClose = function(block) {
	block.dhExpanded = false;;
	var content = document.getElementById("dhStackerBlockContent-" + block.dhBlockId)
	if (content)
		content.style.display = "none";	
	var controls = document.getElementById("dhStackerBlockControls-" + block.dhBlockId)
	if (controls)
		controls.style.display = "none";
	var qual = document.getElementById("dhStackerBlockTimeAgoQualifier-" + block.dhBlockId);
	if (qual)
		qual.style.display = "none";			
	var fullDesc = document.getElementById("dhStackerBlockDescription-" + block.dhBlockId);
	var shortDesc = document.getElementById("dhStackerBlockHeaderDescription-" + block.dhBlockId);
	if (shortDesc) {
		fullDesc.style.display = "none";
		shortDesc.style.display = "block";
	}
	var message = document.getElementById("dhMusicBlockMessage-" + block.dhBlockId);
	if (message)
		message.style.display = "block";
}

dh.stacker.onBlockMouseOver = function(e) {
	if (!e) e = window.event;
	var block = this;
	dh.log("stacker-cursor", "block " + block.dhBlockId + " mouseover");
	if (!dh.util.hasClass(block, "dh-box-prelighted"))
		dh.util.prependClass(block, "dh-box-prelighted");
	var expandImg = document.getElementById("dhStackerBlockExpandTip");
	expandImg.style.display = "block";		
	dh.stacker.updatePointer(block);	
}

dh.stacker.hideBlockPointer = function(block) {
	var expandImg = document.getElementById("dhStackerBlockExpandTip");
	var closeImg = document.getElementById("dhStackerBlockCloseTip");	
	expandImg.style.display = "none";
	closeImg.style.display = "none";
}

dh.stacker.onBlockMouseOut = function(e) {
	if (!e) e = window.event;
	var block = this;
	dh.log("stacker-cursor", "block " + block.dhBlockId + " mouseout");	
	dh.stacker.removePrelight(block);
	dh.stacker.updatePointer(block);	
	dh.stacker.hideBlockPointer(block);
}

dh.stacker.repositionPointer = function (block, e) 
{
	var expandImg = document.getElementById("dhStackerBlockExpandTip");
	var closeImg = document.getElementById("dhStackerBlockCloseTip");	
	var img = block.dhExpanded ? closeImg : expandImg;
	var xOffset = window.pageXOffset ? window.pageXOffset : document.body.scrollLeft;
	var yOffset = window.pageYOffset ? window.pageYOffset : document.body.scrollTop;
	img.style.top = (yOffset + e.clientY - 11) + "px"
	img.style.left = (xOffset + e.clientX - 11) + "px";
	dh.log("stacker-cursor", "block: " + block.dhBlockId + " position: left: " + img.style.left + " top: " + img.style.top)
}

dh.stacker.onBlockMouseMove = function(e) {
	if (!e) e = window.event;
	var block = this;
	dh.stacker.repositionPointer(block, e);
}

dh.stacker.updatePointer = function(block) {
	var expandImg = document.getElementById("dhStackerBlockExpandTip");
	var closeImg = document.getElementById("dhStackerBlockCloseTip");	
	var img;
	var prevImg;
	if (block.dhExpanded) {
		img = closeImg;
		prevImg = expandImg;
	} else {
		img = expandImg;
		prevImg = closeImg;
	}		
	img.style.display = "block";
	prevImg.style.display = "none";		
}

dh.stacker.onBlockClick = function(e) {
	if (!e) e = window.event;
	var block = this;
	dh.log("stacker", "block " + block.dhBlockId + " click")	
	if (block.dhExpanded) {
		dh.stacker.blockClose(block)
	} else {
		dh.stacker.blockOpen(block)
	}
	dh.stacker.updatePointer(block);
	dh.stacker.repositionPointer(block, e);
}

dh.stacker.hookLinkChildren = function(block, startNode) {
	var i;
	if (startNode.nodeType != 1)
		return;
	for (i = 0; i < startNode.childNodes.length; i++) {
		var node = startNode.childNodes[i];
		if (node.nodeType != 1)
			continue;		
		if (node.nodeName.toLowerCase() == "a") {
			node.onmouseover = function (e) {
				if (!e) e = window.event;
				dh.log("stacker-cursor", "block: " + block.dhBlockId + " link mouseover")				
				dh.stacker.hideBlockPointer(block);
				dh.util.cancelEvent(e);
			};
			node.onmouseout = function (e) {
				if (!e) e = window.event;
				var relTarget = e.relatedTarget || e.toElement;				
				dh.log("stacker-cursor", "block: " + block.dhBlockId + " link mouseout to " + relTarget)					
				if (!dh.util.isDescendant(block, relTarget))
					dh.stacker.hideBlockPointer(block);				
				dh.util.cancelEvent(e);			
			};			
		} else {
			dh.stacker.hookLinkChildren(block, node);
		}
	}
}

dh.stacker.insertBlockHeaderDescription = function(blockId) {
	var fullDesc = document.getElementById("dhStackerBlockDescription-" + blockId);
	var shortDesc = document.getElementById("dhStackerBlockHeaderDescription-" + blockId);
	
	var text = dh.util.getTextFromHtmlNode(fullDesc);
	shortText = text.substring(0, 70);
	shortDesc.appendChild(document.createTextNode(shortText));
	if (text.length > shortText.length)
		shortDesc.appendChild(document.createTextNode("..."));
	fullDesc.style.display = "none";
}
