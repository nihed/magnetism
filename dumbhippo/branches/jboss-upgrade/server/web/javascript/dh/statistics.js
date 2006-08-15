dojo.provide("dh.statistics");

dojo.require("dh.statistics.dataset");
dojo.require("dh.statistics.chart");
dojo.require("dh.statistics.selector");
dojo.require("dh.util");

dh.statistics.graph1 = null;

function dhStatisticsInit() {
	dh.statistics.graph1 = new dh.statistics.chart.Chart(600, 200, "dhCoords1");
	document.getElementById("dhGraph1").appendChild(dh.statistics.graph1.getCanvas());
    dh.statistics.onSelectedColumnChange();
}

dh.statistics.onSelectedColumnChange = function() {
    var columnSelect = document.getElementById("dhColumnSelect");
    var columnSelectIndex = columnSelect.selectedIndex;
	dh.server.doXmlMethod("statistics",
				     { "columns" : columnSelect.options[columnSelectIndex].id },
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
