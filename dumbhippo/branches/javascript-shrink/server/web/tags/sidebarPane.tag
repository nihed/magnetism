<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ attribute name="title" required="false" type="java.lang.String"%>
<%@ attribute name="last" required="false" type="java.lang.Boolean"%>
<c:choose>
	<c:when test="${last}">
		<div class="dh-right-box dh-right-box-last">
	</c:when>
	<c:otherwise>
		<div class="dh-right-box">
	</c:otherwise>
</c:choose>
	<c:if test="${!empty title}">
		<h5 class="dh-title"><c:out value="${title}"/></h5>
	</c:if>
	<jsp:doBody/>
</div>
