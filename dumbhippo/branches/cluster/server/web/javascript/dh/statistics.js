dojo.provide("dh.statistics");

dojo.require("dh.statistics.dataset");
dojo.require("dh.statistics.chart");
dojo.require("dh.statistics.selector");
dojo.require("dh.util");

dh.statistics.graph1 = null;

function dhStatisticsInit() {
	dh.statistics.graph1 = new dh.statistics.chart.Chart(600, 200, "dhCoords1");
	document.getElementById("dhGraph1").appendChild(dh.statistics.graph1.getCanvas());
    dh.statistics.onSelectedFileChange();
}

dh.statistics._getSelectedSetFilename = function() {
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
	return this.servers[serverSelect.selectedIndex];
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
				     { "filename" : this._getSelectedSetFilename() },
				       function(childNodes, http) {
				           var dataset = new dh.statistics.dataset.Dataset();
				     	   // switch to the child nodes of the statisticsSet
				     	   var childNodes = childNodes.item(0).childNodes.item(0).childNodes;
					       var i = 0;
					       for (i = 0; i < childNodes.length; ++i) {
					       	   // find a child node named "columns"
					           var child = childNodes.item(i);
					           if (child.nodeName == "columns") {
					        	   columnNodes = child.childNodes;
					               var j = 0;
					               for (j = 0; j < columnNodes.length; ++j) {
					                   column = columnNodes.item(j);
					                   // add new option, select the one that was previously selected, if one exists,
					                   // select the first one by default
					           	       columnSelect.options[j] = 
					        		       new Option(dojo.dom.textContent(column.firstChild) + " -- " + column.getAttribute("units") + " -- " + column.getAttribute("type"),
					        		                  "",
					        		                  (j == 0),
					        		                  (column.getAttribute("id") == oldSelectedColumnId));	
					        		   columnSelect.options[j].id = column.getAttribute("id");               				        		                     			        	    
					        	   }
					        	   break;
					           }
					       }	
					       dh.statistics.onSelectedColumnChange();
			           },
		  	    	   function(code, msg, http) {
		  	    	      alert("Cannot load data for the selected file.");
		  	    	   });
}
	
dh.statistics.onSelectedServerChange = function() {
	dh.statistics.onSelectedColumnChange();
}

dh.statistics.onSelectedColumnChange = function() {
    var columnSelect = document.getElementById("dhColumnSelect");
    var columnSelectIndex = columnSelect.selectedIndex;
	// we need to use GET rather than
    // the default POST because "runOnServer" only works with GET. (Implementation
    // limitation in the server ... proxying a POST is harder.)	
    
    var params = {}
    
    params.columns = columnSelect.options[columnSelectIndex].id
    if (this._getSelectedServer() == this.thisServer)
    	params.filename = this._getSelectedSetFilename()
	else
		params.runOnServer = this._getSelectedServer();
    
	dh.server.doXmlMethodGET("statistics",
	                    params,
				     	function(childNodes, http) {
				     		var dataset = new dh.statistics.dataset.Dataset();
				     	    // switch to the childNodes that represent rows
				     	    var childNodes = childNodes.item(0).childNodes;
					        var i = 0;
					        for (i = 0; i < childNodes.length; ++i) {
						        var child = childNodes.item(i);					        
						        dataset.add(parseInt(child.getAttribute("time")), parseInt(dojo.dom.textContent(child))); 
						    }
						    
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
       					    
       				        dh.statistics.graph1.setDataset(dataset);				    
		  	    	     },
		  	    	     function(code, msg, http) {
		  	    	        alert("Cannot load data from server.");
		  	    	     });
}	
