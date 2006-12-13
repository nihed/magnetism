<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ attribute name="value" required="true" type="java.lang.String"%>
<%@ attribute name="canModify" required="true" type="java.lang.Boolean"%>
<div id="dhName">
	<c:choose>
		<c:when test="${canModify}">	
		    <dht:userNameEdit value="${value}"/>
		</c:when>
		<c:otherwise>
		    <c:out value="${value}"/>
		</c:otherwise>
	</c:choose>
</div>
