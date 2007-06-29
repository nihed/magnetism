dojo.provide("dojo.collections.Dictionary");
dojo.require("dojo.collections.Collections");

dojo.collections.Dictionary = function(dictionary){
	var items = {};
	this.count = 0;
	if (dictionary){
		var e = dictionary.getIterator();
		while (e.moveNext()) {
			 items[e.key] = new dojo.collections.DictionaryEntry(e.key, e.value);
		}
	}

	this.add = function(k,v){
		items[k] = new dojo.collections.DictionaryEntry(k,v);
		this.count++;
	};
	this.clear = function(){
		items = {};
		this.count = 0;
	};
	this.clone = function(){
		return new dojo.collections.Dictionary(this);
	};
	this.contains = this.containsKey = function(k){
		return (items[k] != null);
	};
	this.containsValue = function(v){
		var e = this.getIterator();
		while (e.moveNext()) {
			if (e.value == v) return true;
		}
		return false;
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
	this.item = function(k){
		return items[k];
	};
	this.getIterator = function(){
		return new dojo.collections.DictionaryIterator(items);
	};
	this.remove = function(k){
		delete items[k];
		this.count--;
	};
};
