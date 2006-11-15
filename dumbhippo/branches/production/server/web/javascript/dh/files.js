dojo.provide('dh.files');
dojo.require('dh.server');

dh.files.deleteFile = function(fileId) {
	dh.server.doXmlMethod("deleteFile",
						{ "fileId": fileId },
			  	    	function(childNodes, http) {
			  	    		document.location.reload();
						},
		  	    	 	function(code, msg, http) {
							alert(msg);
						});
}
