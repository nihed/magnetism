<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="multiline" required="false" type="java.lang.Boolean" %>
<%@ attribute name="extraClass" required="false" type="java.lang.String" %>

<c:choose>
	<c:when test="${multiline}">
		<textarea class="dh-text-input ${extraClass}"></textarea>
	</c:when>
	<c:otherwise>
		<input type="text" class="dh-text-input ${extraClass}"/>
	</c:otherwise>
</c:choose>
