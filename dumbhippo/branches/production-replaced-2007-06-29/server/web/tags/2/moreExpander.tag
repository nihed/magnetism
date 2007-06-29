<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="open" required="true" type="java.lang.Boolean" %>
<%-- FIXME make this required when we're using it --%>
<%@ attribute name="more" required="false" type="java.lang.String" %>
<%@ attribute name="text" required="false" type="java.lang.String" %>

<c:if test="${empty text}">
	<c:set var="text" value="more"/>
</c:if>

<c:choose>
	<c:when test="${open}">
		<c:set var="image" value="/images2/${buildStamp}/arrow_down.gif" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="image" value="/images2/${buildStamp}/arrow_right.gif" scope="page"/>
	</c:otherwise>
</c:choose>

<div class="dh-${text}"><a href="${more}"><c:out value="${fn:toUpperCase(text)}"/></a> <a href="${more}"><img src="${image}"/></a></div>
