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

function dhStatisticsInit() {
	dh.statistics._fetcher = new dh.statistics.fetcher.Fetcher();
	dh.statistics._fetcher.onFetch = function(ids) {
		dh.statistics._onFetch(ids);
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
	for (var i = 0; i < ids.length; i++) {
		var id = ids[i];
		var block = this._blocks[id];
		if (!block)
			continue;
			
		var dataset = this._fetcher.getDataset(id);
		block.setDataset(dataset);
	}
}

dh.statistics._onSpecificationChanged = function(id) {
	var block = this._blocks[id];
	this._fetcher.setSpecification(id, block.getSpecification());
}
