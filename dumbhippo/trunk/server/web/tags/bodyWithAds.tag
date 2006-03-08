<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<dht:body fixedHack="true">
<jsp:doBody/>
<dht:bottom/>
<dht:fixed>
	<div id="dhOTP">
		<dht:ad src="${psa1}"/>
		<br/>
		<dht:ad src="${psa2}"/>
	</div>
</dht:fixed>
</dht:body>
