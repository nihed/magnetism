<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title><c:out value="${flashMessage}"/></title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
	<dht:scriptIncludes/>
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
	<script type="text/javascript">
		dojo.require("dh.util");
	
		dh.util.goToNextPage("<c:out value="${next}"/>", "<c:out value="${flashMessage}"/>");
	</script>
</head>
<body>
	<dht:header>
	</dht:header>

	<div id="main">
		<c:out value="${flashMessage}"/>
	</div>
</body>
</html>
