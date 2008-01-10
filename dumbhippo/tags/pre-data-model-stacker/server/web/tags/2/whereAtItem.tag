<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="label" required="false" type="java.lang.String" %>
<%@ attribute name="linkText" required="true" type="java.lang.String" %>
<%@ attribute name="linkTarget" required="true" type="java.lang.String" %>

<c:if test="${!empty label}">
	<span class="dh-where-at-label"><c:out value="${label}"/>:</span>
</c:if>
<a class="dh-where-at-link" href="${linkTarget}"><c:out value="${linkText}"/></a>
