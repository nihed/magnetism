<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title><c:out value="${!empty flashMessage ? flashMessage : 'Mugshot'}"/></title>
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
	
		dh.util.goToNextPage("<c:out value="${next}"/>", "<c:out value="${flashMessage}"/>");
	</script>
</head>
<dht:systemPage topText="Message" disableJumpTo="true">
	<c:out value="${!empty flashMessage ? flashMessage : 'Closing window...'}"/>
</dht:systemPage>	
</html>
