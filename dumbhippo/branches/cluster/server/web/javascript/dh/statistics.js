dojo.provide("dh.statistics");

dojo.require("dh.statistics.chart");
dojo.require("dh.statistics.fetcher");
dojo.require("dh.statistics.dataset");
dojo.require("dh.statistics.selector");
dojo.require("dh.statistics.set");
dojo.require("dh.util");

dh.statistics._graph1 = null;

function dhStatisticsInit() {
	dh.statistics._fetcher = new dh.statistics.fetcher.Fetcher();
	dh.statistics._fetcher.onFetch = function(ids) {
		dh.statistics._onFetch(ids);
	}
	
	dh.statistics._graph1 = new dh.statistics.chart.Chart(600, 200, "dhCoords1");
	document.getElementById("dhGraph1").appendChild(dh.statistics._graph1.getCanvas());
	
    dh.statistics.onSelectedFileChange();
}

dh.statistics._getSelectedFilename = function() {
    var fileSelect = document.getElementById("dhFileSelect");
	return fileSelect.value;
}

dh.statistics._isSetCurrent = function() {
    var fileSelect = document.getElementById("dhFileSelect");
    // The current set is always the first one
    return fileSelect.selectedIndex == 0;
}

dh.statistics._getSelectedServer = function() {
	var serverSelect = document.getElementById("dhServerSelect");
	return serverSelect.value;
}

dh.statistics._getSelectedColumn = function() {
	var columnSelect = document.getElementById("dhColumnSelect");
	return columnSelect.value;
}

// Offer a choice of servers if the statistics set is the current set
// (the server information might be inaccurate for older filesets, and 
// looking at different servers there is less interesting in any case.)
dh.statistics._fillServerSelect = function(serverSelect) {
    serverSelect.options.length = 0;
    
    var oldIndex = serverSelect.selectedIndex;
	if (this._isSetCurrent()) {
		serverSelect.disabled = false;
		for (var i = 0; i < this.servers.length; i++) {
			var defaultSelected = this.servers[i] == this.thisServer;
			var selected = oldIndex >= 0 ? i == oldIndex : defaultSelected;
			serverSelect.options[i] = new Option(this.servers[i],
			                                     this.servers[i],
			                                     defaultSelected,
			                                     selected);
		}
	} else {
		serverSelect.disabled = true
		serverSelect.options[0] = new Option(dh.statistics.thisServer,
											 dh.statistics.thisServer,
											 true,
											 true);
	}
}

dh.statistics._fillColumnSelect = function(set) {
	var columnSelect = document.getElementById("dhColumnSelect");
	var oldSelected = columnSelect.value
	
	columnSelect.options.length = 0;
	for (var i = 0; i < set.length; i++) {
		var column = set.item(i);
		var defaultSelected = i == 0;
		var selected = oldSelected ? column.id == oldSelected : defaultSelected;
		
		columnSelect.options[i] = 
			new Option(column.name, column.id, defaultSelected, selected);
	}
}


dh.statistics._updateFetcher = function() {
	var server = this._getSelectedServer();
	var filename = this._getSelectedFilename();
	var column = this._getSelectedColumn();

	var specification = new dh.statistics.fetcher.Specification(server, filename, column);
	this._fetcher.setSpecification("chart1", specification);
}

dh.statistics.onSelectedFileChange = function() {
	this._fillServerSelect(document.getElementById("dhServerSelect"));

    var columnSelect = document.getElementById("dhColumnSelect");
    var columnSelectIndex = columnSelect.selectedIndex;
    // preserve the option that was currently selected
    var oldSelectedColumnId = "";
    if (columnSelectIndex >= 0)
        oldSelectedColumnId = columnSelect.options[columnSelectIndex].id
    // remove all options from the column select
    columnSelect.options.length = 0;
    		  	    	   
    // get statistics set for a given file
	dh.server.doXmlMethodGET("statisticssets",
				     { "filename" : this._getSelectedFilename() },
				       function(childNodes, http) {
				       	   var server = dh.statistics.thisServer;
				       	   var filename = dh.statistics._getSelectedServer();
				       	   var set = dh.statistics.set.fromXml(childNodes.item(0).firstChild, server, filename);
				       	   dh.statistics._fillColumnSelect(set);
				           dh.statistics._updateFetcher();
			           },
		  	    	   function(code, msg, http) {
		  	    	      alert("Cannot get statistics sets: " + msg);
		  	    	   });
}
	
dh.statistics.onSelectedServerChange = function() {
	this._updateFetcher();
}
 
dh.statistics.onSelectedColumnChange = function() {
	this._updateFetcher();
}

dh.statistics._onFetch = function(ids) {
	var dataset = this._fetcher.getDataset("chart1");

    var startTimeDiv = document.getElementById("dhStartTime"); 
    if (dataset.minT == 0) {
 	    startTimeDiv.replaceChild(document.createTextNode(""), startTimeDiv.firstChild);       					    
    } else {
 	    startTimeDiv.replaceChild(document.createTextNode(dh.util.timeString(dataset.minT)), startTimeDiv.firstChild);
    }           

	var endTimeDiv = document.getElementById("dhEndTime");
	if (dataset.minT == dataset.maxT) {
	    endTimeDiv.replaceChild(document.createTextNode(""), endTimeDiv.firstChild);
 	} else {
	    endTimeDiv.replaceChild(document.createTextNode(dh.util.timeString(dataset.maxT)), endTimeDiv.firstChild);
 	}
    					    
    var minValDiv = document.getElementById("dhMinVal");
    // this will display 0 next to the origin point if there is no data in the dataset
    minValDiv.replaceChild(document.createTextNode(dataset.minY), minValDiv.firstChild);
    var maxValDiv = document.getElementById("dhMaxVal");
    if (dataset.minY == dataset.maxY) {
        maxValDiv.replaceChild(document.createTextNode(""), maxValDiv.firstChild);     
    } else {        					        
        maxValDiv.replaceChild(document.createTextNode(dataset.maxY), maxValDiv.firstChild);       	
    }
       					    
    dh.statistics._graph1.setDataset(dataset);
}
