dojo.provide("dojo.collections.SortedList");
dojo.require("dojo.collections.Collections");

dojo.collections.SortedList = function(dictionary){
	var items = {};
	var q = [];
	var sorter = function(a,b){
		if (a.key > b.key) return 1;
		if (a.key < b.key) return -1;
		return 0;
	};
	var build = function(){
		var e = this.getIterator();
		while (e.moveNext()) {
			q.push(e.entry);
		}
		q.sort(sorter);
	};

	if (dictionary){
		var e = dictionary.getIterator();
		while (e.moveNext()) {
			q[q.length] = items[e.key] = new dojo.collections.DictionaryEntry(e.key, e.value);
		}
		q.sort(sorter);
	}

	this.count = q.length;
	this.add = function(k,v){
		if (!items[k]) {
			items[k] = new dojo.collections.DictionaryEntry(k,v);
			this.count = q.push(items[k]);
			q.sort(sorter);
		}
	};
	this.clear = function(){
		items = {};
		q = [];
		this.count = q.length;
	};
	this.clone = function(){
		return new dojo.collections.SortedList(this);
	};
	this.contains = this.containsKey = function(k){
		return (items[k] != null);
	};
	this.containsValue = function(o){
		var e = this.getIterator();
		while (e.moveNext()){
			if (e.value == o) return true;
		}
		return false;
	};
	this.copyTo = function(arr, i){
		var e = this.getIterator();
		var idx = i;
		while (e.moveNext()){
			arr.splice(idx, 0, e.entry);
			idx++;
		}
	};
	this.getByIndex = function(i){
		return q[i].value;
	};
	this.getIterator = function(){
		return new dojo.collections.DictionaryIterator(items);
	};
	this.getKey = function(i){
		return q[i].key;
	};
	this.getKeyList = function(){
		var arr = [];
		var e = this.getIterator();
		while (e.moveNext()) {
			arr.push(e.key);
		}
		return arr;
	};
	this.getValueList = function(){
		var arr = [];
		var e = this.getIterator();
		while (e.moveNext()) {
			arr.push(e.value);
		}
		return arr;
	};
	this.indexOfKey = function(k){
		for (var i = 0; i < q.length; i++){
			if (q[i].key == k) {
				return i;
			}
		}
		return -1;
	};
	this.indexOfValue = function(o){
		for (var i = 0; i < q.length; i++){
			if (q[i].value == o) {
				return i;
			}
		}
		return -1;
	};
	this.item = function(k){
		return items[k];
	};

	this.remove = function(k){
		delete items[k];
		build();
		this.count = q.length;
	};
	this.removeAt = function(i){
		delete items[q[i].key];
		build();
		this.count = q.length;
	};

	this.setByIndex = function(i,o){
		items[q[i].key] = o;
	};
}
