<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>New Photo</title>
	<dht:stylesheets />
	<dht:scriptIncludes/>	
</head>
<body>
	<dht:header>
		New Photo
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<p>Your new photo looks like this (in two sizes):</p>
		<dh:png klass="dh-headshot" src="/files${photoLocation}/192/${photoFilename}?v=${photoVersion}" style="width: 192; height: 192; border: 1px solid white;"/>
		&nbsp;
		<dh:png klass="dh-headshot" src="/files${photoLocation}/48/${photoFilename}?v=${photoVersion}" style="width: 48; height: 48; border: 1px solid white;"/>
		<br/>
		<p><c:out value="${homePageLink}" escapeXml="false"/></p>

		<br/>
		<p>If you hate this photo, you can try again:</p>
		<!--  groupId gets submitted here along with a person photo, but 
			the servlet ignores it -->
		<dht:uploadPhoto location="${photoLocation}" groupId="${photoFilename}"/>
	</div>
</body>
</html>
