<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh"%>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht"%>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3"%>

<%@ attribute name="n" required="true" type="java.lang.Number"%>
<%@ attribute name="s" required="true" type="java.lang.String"%>
<%@ attribute name="p" required="false" type="java.lang.String"%>

<c:out value="${n}"/>
<c:choose>
	<c:when test="${n != 1}">
		<c:choose>
			<c:when test="${!empty p}">
				<c:out value="${p}"/>
			</c:when>
			<c:otherwise>
				<c:out value="${s}"/>s
			</c:otherwise>
		</c:choose>
	</c:when>
	<c:otherwise>
		<c:out value="${s}"/>
	</c:otherwise>
</c:choose>
