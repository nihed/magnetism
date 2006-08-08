dojo.provide("dh.statistics");

dojo.require("dh.statistics.dataset");
dojo.require("dh.statistics.chart");
dojo.require("dh.statistics.selector");

dh.statistics.graph1 = null;

function dhStatisticsInit() {
	dh.statistics.graph1 = new dh.statistics.chart.Chart(600, 200);
	document.getElementById("dhGraph1").appendChild(dh.statistics.graph1.getCanvas());
    dh.statistics.onSelectedColumnChange();
}

dh.statistics.onSelectedColumnChange = function() {
    columnSelect = document.getElementById("dhColumnSelect");
    columnSelectIndex = columnSelect.selectedIndex;
	dh.server.doXmlMethod("statistics",
				     { "columns" : columnSelect.options[columnSelectIndex].id },
				     	function(childNodes, http) {
				     		var dataset = new dh.statistics.dataset.Dataset();
				     	    // switch to the childNodes that represent rows
				     	    childNodes = childNodes.item(0).childNodes;
					        var i = 0;
					        var startTime = null;
					        var endTime = null;
					        for (i = 0; i < childNodes.length; ++i) {
						        var child = childNodes.item(i);					        
						        dataset.add(child.getAttribute("time"), dojo.dom.textContent(child));
						        if (i == 0)
						            startTime = child.getAttribute("time")
						        endTime = child.getAttribute("time")    
						    }
					 	    var date = new Date();
					 	    if ((startTime != null) && (endTime != null)) {
       					 	    startTimeDiv = document.getElementById("dhStartTime"); 
       					 	    startTimeDiv.replaceChild(document.createTextNode(dh.statistics.timeString(startTime)), startTimeDiv.firstChild);
       					 	    endTimeDiv = document.getElementById("dhEndTime");
       					 	    if (startTime == endTime) {
       					 	        endTimeDiv.replaceChild(document.createTextNode(""), endTimeDiv.firstChild);
       					 	    } else {
       					 	        endTimeDiv.replaceChild(document.createTextNode(dh.statistics.timeString(endTime)), endTimeDiv.firstChild);
       					 	    }
       					    }
       					    minValDiv = document.getElementById("dhMinVal");
       					    minValDiv.replaceChild(document.createTextNode(dataset.minY), minValDiv.firstChild);
       					    maxValDiv = document.getElementById("dhMaxVal");
       					    if (dataset.minY == dataset.maxY) {
       					        maxValDiv.replaceChild(document.createTextNode(""), maxValDiv.firstChild);     
       					    } else {        					        
       					        maxValDiv.replaceChild(document.createTextNode(dataset.maxY), maxValDiv.firstChild);       	
       					    }
       				        dh.statistics.graph1.setDataset(dataset);				    
		  	    	     },
		  	    	     function(code, msg, http) {
		  	    	        document.location.reload();
		  	    	     });
}	

dh.statistics.timeString = function(timestamp) {
	var date = new Date();
	date.setTime(timestamp);
    return dh.util.makeTwoDigit(date.getMonth()+1) + "/" + dh.util.makeTwoDigit(date.getDate()) + "/" 
           + (1900 + date.getYear()) + " " + dh.util.makeTwoDigit(date.getHours()) + ":" 
           + dh.util.makeTwoDigit(date.getMinutes()) + ":" + dh.util.makeTwoDigit(date.getSeconds());
}
