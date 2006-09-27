<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="selected" required="true" type="java.lang.String" %>
<%@ attribute name="link" required="true" type="java.lang.String" %>

<c:choose>
	<c:when test="${name != selected}">
		<a href="${link}">${name}</a>
	</c:when>
	<c:otherwise>
		<span class="dh-page-option-selected">${name}</span>
	</c:otherwise>
</c:choose>