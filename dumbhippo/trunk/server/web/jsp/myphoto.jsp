<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Changing Your Photo</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
</head>
<body>
	<dht:header>
		Changing Your Photo
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<br/>
		<br/>
		<dht:uploadPhoto location="/headshots"/>
	</div>
</body>
</html>
