dojo.provide("dh.statistics.set");

dh.statistics.set.Set = function(server, filename) {
	this.server = server;
	this.filename = filename;
	this._columns = [];
	this.length = 0;
}

dojo.lang.extend(dh.statistics.set.Set,
{
	add: function(id, name, units, type) {
		this._columns.push({
			id: id,
			name: name,
			units: units,
			type: type
		});
		this.length++;
	},
	
	item: function(index) {
		return this._columns[index];
	},
});

dh.statistics.set.fromXml = function(statisticsSetElement, server, filename) {
	var set = new dh.statistics.set.Set();

 	var childNodes = statisticsSetElement.childNodes;
 	
 	// find a child node named "columns"
 	var columnNodes = null;
    for (var i = 0; i < childNodes.length; i++) {
        var child = childNodes.item(i);
        if (child.nodeName == "columns") {
        	columnNodes = child.childNodes;
        	break;
        }    	     
 	}
 	
 	if (!columnNodes) {
 		alert("Can't find <columns/> element in result");
 		return null;
 	}

	for (var j = 0; j < columnNodes.length; j++) {
        var column = columnNodes.item(j);
        set.add(column.getAttribute("id"),
				dojo.dom.textContent(column.firstChild),
				column.getAttribute("units"),
			    column.getAttribute("type"));
    }
    
    return set;
}
