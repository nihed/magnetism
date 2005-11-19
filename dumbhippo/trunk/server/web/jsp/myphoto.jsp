<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Changing Your Photo</title>
	<dht:stylesheets />
</head>
<body>
	<dht:header>
		Changing Your Photo
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<p>Choose a new photo file:</p>
		<dht:uploadPhoto location="/headshots"/>
	</div>
</body>
</html>
