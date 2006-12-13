dojo.provide("dojo.io.RhinoIO");

dojo.io.SyncHTTPRequest = function(){
	dojo.io.SyncRequest.call(this);

	this.send = function(URI){
	}
}

dojo.inherits(dojo.io.SyncHTTPRequest, dojo.io.SyncRequest);

