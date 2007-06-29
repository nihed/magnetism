dojo.provide("dh.statistics");

dojo.require("dh.statistics.block");
dojo.require("dh.statistics.fetcher");
dojo.require("dh.statistics.dataset");
dojo.require("dh.statistics.selector");
dojo.require("dh.statistics.set");
dojo.require("dh.util");

dh.statistics._blocks = {};
dh.statistics._nextBlockId = 1;
dh.statistics._set = null;

dhStatisticsInit = function() {
	window.addEventListener("keypress", dh.statistics._onKeyPress, true);

	dh.statistics._fetcher = new dh.statistics.fetcher.Fetcher();
	dh.statistics._fetcher.onFetch = function(ids) {
		dh.statistics._onFetch(ids);
	}
	
	dh.statistics._selectionStart = null;
	dh.statistics._selectionEnd = null;
	
	dh.statistics._selector = new dh.statistics.selector.Selector();
    document.getElementById("dhHourSelector").appendChild(dh.statistics._selector.table);
	
	dh.statistics._selector.onSelectionChanged = function(start, end) {
		dh.statistics._onSelectionChanged(start, end);
	}

	dh.statistics.addBlock(false);
	
    dh.statistics.onSelectedFileChange();
}

dh.statistics._getSelectedFilename = function() {
    var fileSelect = document.getElementById("dhFileSelect");
	return fileSelect.value;
}

dh.statistics.onSelectedFileChange = function() {
    // get statistics set for a given file
	dh.server.doXmlMethodGET("statisticssets",
				     { "filename" : this._getSelectedFilename(),
				       "runOnServer" : this.thisServer },
				       function(childNodes, http) {
				       	   var server = dh.statistics.thisServer;
				       	   var filename = dh.statistics._getSelectedFilename();
				       	   var set = dh.statistics.set.fromXml(childNodes.item(0).firstChild, server, filename);
				       	   
				       	   dh.statistics._setSet(set);
			           },
		  	    	   function(code, msg, http) {
		  	    	      alert("Cannot get statistics sets: " + msg);
		  	    	   });
}

dh.statistics.addBlock = function() {
	var block = new dh.statistics.block.Block();
	var id = this._nextBlockId++;
	var table = block.getTable();

	this._blocks[id] = block;
	
	block.onSpecificationChanged = function() {
		dh.statistics._onSpecificationChanged(id);
	};
	
	block.onRemove = function() {
		table.parentNode.removeChild(table);
		dh.statistics._fetcher.remove(id);
		delete dh.statistics._blocks[id];
	};
	
	document.getElementById("dhBlocksDiv").appendChild(table);
	
	if (this._set)
		block.setSet(this._set);
}

dh.statistics.refresh = function() {
	this._fetcher.refetchAll();
}

dh.statistics._setSet = function(set) {
	this._set = set;

   	for (var id in this._blocks) {
   	     var block = this._blocks[id];
       	 block.setSet(set);
   	}
}
	
dh.statistics._onFetch = function(ids) {
	var	minT = null;
    var	maxT = null;
	for (var id in this._blocks) {
		var setStartTime = this._fetcher.getSetStartTime(id);
		var setEndTime = this._fetcher.getSetEndTime(id);
		var dataset = this._fetcher.getDataset(id);
		
		if (setStartTime != null && (minT == null || minT > setStartTime))
			minT = setStartTime;
		if (setEndTime != null && (maxT == null || maxT < setEndTime))
			maxT = setEndTime;
	}

	this._setsStartTime = minT;
	this._setsEndTime = maxT;

	for (var i = 0; i < ids.length; i++) {
		var id = ids[i];
		var block = this._blocks[id];
		if (!block)
			continue;
			
		var dataset = this._fetcher.getDataset(id);
		block.setDataset(dataset);
	}

	this._updateBlockRanges();	
	this._selector.setRange(minT, maxT);
}

dh.statistics._updateBlockRanges = function() {
	var selectionStart;
	if (this._selectionStart != null && this._selectionStart > this._setsStartTime)
		selectionStart = this._selectionStart;
	else
		selectionStart = this._setsStartTime;
		
	var selectionEnd;
	if (this._selectionEnd != null && this._selectionEnd < this._setsEndTime)
		selectionEnd = this._selectionEnd;
	else
		selectionEnd = this._setsEndTime;

	for (var id in this._blocks) {
		this._blocks[id].setRange(selectionStart, selectionEnd);
	}
}

dh.statistics._onSpecificationChanged = function(id) {
	var block = this._blocks[id];
	this._fetcher.setSpecification(id, block.getSpecification());
}

dh.statistics._onSelectionChanged = function(start, end) {
	if (start == this._selectionStart && end == this._selectionEnd)
		return;

	this._selectionStart = start;
	this._selectionEnd = end;
	
	this._fetcher.setSelection(start, end);
	
	// Redraw the blocks with the current data, when the new fetch
	// comes in we'll do it again with better data
	this._updateBlockRanges();	
}

dh.statistics._onKeyPress = function(e) {
	// Hook up Shift-Alt-D to show the debug log window
	if (e.charCode == 68 && e.shiftKey && e.altKey) {
		dh.logger.show();		
		e.stopPropagation();
		e.preventDefault();	
	}
}