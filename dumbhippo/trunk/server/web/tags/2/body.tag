<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%-- this tag is used for lots of different page templates, so you probably shouldn't add stuff
	to it; instead create a tag for your "kind of page" --%>
<%@ attribute name="extraClass" required="false" type="java.lang.String" %>	
	
<c:choose>
	<c:when test="${showSidebar}">
		<c:set var="bodyClass" value="dh-body-with-sidebar" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="bodyClass" value="dh-body-without-sidebar" scope="page"/>
	</c:otherwise>
</c:choose>
<body class="${bodyClass} ${extraClass}">
	<div id="dhPage">
		<jsp:doBody/>
	</div>
</body>
