<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="open" required="true" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${open}">
		<c:set var="image" value="/images2/arrow_down.gif" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="image" value="/images2/arrow_right.gif" scope="page"/>
	</c:otherwise>
</c:choose>

<div class="dh-more"><a href="">MORE</a> <img src="${image}"/></div>
