dojo.provide("dh.statistics.set");
dojo.require("dh.dom");
dojo.require("dh.util");

dh.statistics.set.Set = function(server, filename, current) {
	this.server = server;
	this.filename = filename;
	this.current = current;
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
 	var childNodes = statisticsSetElement.childNodes;
 	
 	var current = false;
 	var columnNodes = null;
    for (var i = 0; i < childNodes.length; i++) {
        var child = childNodes.item(i);
        if (child.nodeName == "columns") {
        	columnNodes = child.childNodes;
        } else if (child.nodeName == "current") {
			current = dh.util.trim(dh.dom.textContent(child)) == "true";
        }
 	}
 	
	var set = new dh.statistics.set.Set(server, filename, current);

 	if (!columnNodes) {
 		alert("Can't find <columns/> element in result");
 		return null;
 	}

	for (var j = 0; j < columnNodes.length; j++) {
        var column = columnNodes.item(j);
        set.add(column.getAttribute("id"),
				dh.dom.textContent(column.firstChild),
				column.getAttribute("units"),
			    column.getAttribute("type"));
    }
    
    return set;
}
