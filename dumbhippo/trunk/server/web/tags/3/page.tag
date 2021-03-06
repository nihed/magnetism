<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="currentPageLink" required="true" type="java.lang.String" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>
<%@ attribute name="blocks" required="false" type="java.lang.Boolean" %>
<%@ attribute name="onresize" required="false" type="java.lang.String" %>

<body class="dh-gray-background-page" onresize="${onresize}">
	<c:if test="${blocks}">
		<dht3:blockGlobal/>
	</c:if>
	<div id="dhPageOuter">
		<div id="dhPage">
			<dht3:header currentPageLink="${currentPageLink}" searchText="${searchText}"/>
			<jsp:doBody/>
		</div>
		<dht:footer/>
	</div>
	<dht3:analytics/>
</body>
