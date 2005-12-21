<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title><c:out value="${flashMessage}"/></title>
	<dht:stylesheets />
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
	
		dh.util.goToNextPage("<c:out value="${next}"/>", "<c:out value="${flashMessage}"/>");
	</script>
</head>
<body>
	<dht:header>
	</dht:header>

	<div id="dhMain">
		<c:out value="${flashMessage}"/>
	</div>
</body>
</html>
