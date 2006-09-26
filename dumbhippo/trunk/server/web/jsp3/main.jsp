<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css3/${buildStamp}/main.css"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>
<body class="dh-gray-background-page dh-home-page">
	<div id="dhPage">
		<dht3:header/>
		<dht3:shinyBox color="grey"></dht3:shinyBox>
		<dht:footer/>
	</div>
</body>
</html>
