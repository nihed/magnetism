dojo.provide("dh.files");
dojo.require("dh.server");
dojo.require("dh.util");

dh.files.deleteFile = function(fileId) {
	dh.server.doXmlMethod("deleteFile",
						{ "fileId": fileId },
			  	    	function(childNodes, http) {
			  	    		dh.util.refresh();
						},
		  	    	 	function(code, msg, http) {
							alert(msg);
						});
}
