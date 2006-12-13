dojo.provide("dh.statistics.fetcher");
dojo.require("dh.server");

dh.statistics.fetcher.Specification = function(server, filename, column) {
	this.server = server;
	this.filename = filename;
	this.column = column;
}

dh.statistics.fetcher.Fetcher = function() {
	this._datasets = {};
	this._lastFetched = {};
	this._timeout = null;
	this._selectionStart = null;
	this._selectionEnd = null;
}

dojo.lang.extend(dh.statistics.fetcher.Fetcher,
{
	setSpecification: function(id, specification) {
		this._datasets[id] = {
			specification: specification,
			dataset: null
		};
		 
		this._queueIdleFetch();
	},
	
	setSelection: function(start, end) {
		this._selectionStart = start;
		this._selectionEnd = end;
		
		this.refetchAll();		
	},
	
	remove: function(id) {
		delete this.specs[id];
	},
	
	refetchAll: function(id) {
		this._lastFetched = {};
		this._queueIdleFetch();
	},
	
	getDataset: function(id) {
		var dataset = this._datasets[id];
		if (dataset)
			return this._datasets[id].dataset;
		else
			return null;
	},
	
	getSetStartTime: function(id) {
		var dataset = this._datasets[id];
		if (dataset)
			return this._datasets[id].setStartTime;
		else
			return null;
	},
	
	getSetEndTime: function(id) {
		var dataset = this._datasets[id];
		if (dataset)
			return this._datasets[id].setEndTime;
		else
			return null;
	},
	
	onFetch: function(ids) {
	},
	
	_queueIdleFetch: function() {
		var me = this;
		if (!this._timeout) {
			this._timeout = setTimeout(function() {
				me._fetch();
			});
		}
	},
	
	_fetch: function() {
		this._timeout = null;
		var servers = {};
		
		// Construct a tree of what we need to fetch
		//
		//  server => filename => column => id
		//
		for (var id in this._datasets) {
			var specification = this._datasets[id].specification;
			var lastFetched = this._lastFetched[id];
			if (lastFetched && 
				lastFetched.server == specification.server &&
				lastFetched.filename == specification.filename &&
				lastFetched.column == specification.column)
				continue;
				
			this._lastFetched[id] = specification;
			
			if (!servers[specification.server])
				servers[specification.server] = {};
				
			var filenames = servers[specification.server];
			if (!filenames[specification.filename])
				filenames[specification.filename] = {};
				
			var columns = filenames[specification.filename];
			if (!columns[specification.column])
				columns[specification.column] = {};
				
			var ids = columns[specification.column];
			ids[id] = 1;
		}
		
		// Fire off a HTTP request for each server/filename pair
		for (var server in servers) {
			var filenames = servers[server];
			for (var filename in filenames) {
				var columns = filenames[filename];
				this._fetchOne(server, filename, columns);
			}
		}
	},
	
	_fetchOne: function(server, filename, columns) {
		var columnString = "";
		var columnOrder = [];

		for (var column in columns) {
			if (columnString != "")
				columnString += ",";
			columnString += column;
			columnOrder.push(column);
		}
		
		params = {};
		params.columns = columnString;
		if (server == dh.statistics.thisServer)
	    	params.filename = filename;
	    	
		params.runOnServer = server;
		
		if (this._selectionStart != null)
			params.start = this._selectionStart;
		if (this._selectionEnd != null)
			params.end = this._selectionEnd;
    
    	var me = this;
		dh.server.doXmlMethodGET("statistics",
            params,
 	        function(childNodes, http) {
 				me._handleOneResult(childNodes.item(0), server, filename, columns, columnOrder);
  	    	},
		  	function(code, msg, http) {
		  		alert("Cannot load data from server=" + server + ", filename=" + filename + ":\n   " + msg);
		  	});
	},
	
	_handleOneResult: function(topElement, server, filename, columns, columnOrder) {
		// Create one dataset for each column in the result
		var datasets = [];
		var setStartTime = parseFloat(topElement.getAttribute("setStartTime"));
		var setEndTime = parseFloat(topElement.getAttribute("setEndTime"));
		for (var i = 0; i < columnOrder.length; i++) {
			datasets[i] = new dh.statistics.dataset.Dataset();
		}
		
		// Parse the result, add to the datasets
 	    var childNodes = topElement.childNodes;
 	    var first = true;
        for (var i = 0; i < childNodes.length; ++i) {
	        var child = childNodes.item(i);
	        var time = parseFloat(child.getAttribute("time"));
	        var content = dojo.dom.textContent(child);
	        var values = dojo.dom.textContent(child).split(",");
			for (var j = 0; j < values.length; j++) {
				datasets[j].add(time, parseInt(values[j]));
			}
	    }
	    
	    // Store the datasets into our internal store
		var allIds = [];
		for (var i = 0; i < columnOrder.length; i++) {
			var column = columnOrder[i];
			var ids = columns[column];
			for (var id in ids) {
				// We need to check if the specification for this dataset is still what
				// was originally requested before storing the result; another fetch
				// could have been queued in the meantime
				var info = this._datasets[id];
				if (info && 
					info.specification.server == server && 
					info.specification.filename == filename && 
					info.specification.column == column) {
					info.dataset = datasets[i];
					info.setStartTime = setStartTime;
					info.setEndTime = setEndTime;
					allIds.push(id);
				}
			}
		}
		
		// Notify of the new results
		this.onFetch(allIds);
	},
});
