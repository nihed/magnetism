<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<c:set var="showSidebar" value="${signin.valid}" scope="request"/>
<dht:body>
	<dht:header/>
		<div id="dhPageContent">
			<jsp:doBody/>
		</div>
	<dht:footer/>
</dht:body>
