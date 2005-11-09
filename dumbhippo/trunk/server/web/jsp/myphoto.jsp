<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Changing Your Photo</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.server");
	    
	    function setHeadshot() {
	    	var form = document.getElementById("dhHeadshotForm");
	    	dh.server.submitUploadForm(form, "headshots",
				  	    	 function(type, data, http) {
								dojo.debug("submitted OK");
				  	    	 },
				  	    	 function(type, error, http) {
								dojo.debug("submission failed: " + error);
				  	    	 });
	    }
	</script>
</head>
<body>
	<dht:header>
		Changing Your Photo
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<br/>
		<br/>
		<form id="dhHeadshotForm" enctype="multipart/form-data">
			<input id="headshotFileEntry" type="file" name="photo"/>
			<br/>
			<input type="button" onclick="setHeadshot();" value="Do it!"/>
		</form>
		
		<div id="dojoDebug"/> <!-- where to put dojo debug spew -->
	</div>
</body>
</html>
