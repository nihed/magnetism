<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<dht:body fixedHack="true">
<jsp:doBody/>
<dht:bottom/>
<dht:fixed>
	<div id="dhOTP">
		<%-- psa1 is empty if we e.g. forward to an error page from AbstractServlet
			instead of RewriteServlet --%>
		<c:if test="${!empty psa1}">
			<dht:ad src="${psa1}"/>
			<br/>
			<dht:ad src="${psa2}"/>
		</c:if>
	</div>
</dht:fixed>
</dht:body>
