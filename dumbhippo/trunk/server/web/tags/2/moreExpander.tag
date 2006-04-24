<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="open" required="true" type="java.lang.Boolean" %>
<%-- FIXME make this required when we're using it --%>
<%@ attribute name="more" required="false" type="java.lang.String" %>

<c:choose>
	<c:when test="${open}">
		<c:set var="image" value="/images2/arrow_down.gif" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="image" value="/images2/arrow_right.gif" scope="page"/>
	</c:otherwise>
</c:choose>

<div class="dh-more"><a href="${more}">MORE</a> <a href="${more}"><img src="${image}"/></a></div>
