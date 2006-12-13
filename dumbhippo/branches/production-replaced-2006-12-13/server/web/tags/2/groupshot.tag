<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.server.views.GroupView"%>
<%@ attribute name="size" required="false" type="java.lang.String" %>
<%@ attribute name="customLink" required="false" type="java.lang.String" %>

<%-- this is required or the dh:png won't have a size set --%>
<c:if test="${empty size}">
	<c:set var="size" value="60" scope="page"/>
</c:if>

<c:choose>
	<c:when test="${size == 30}">
		<c:set var="photoUrl" value="${group.photoUrl30}" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="photoUrl" value="${group.photoUrl60}" scope="page"/>
	</c:otherwise>
</c:choose>
<c:choose>
        <c:when test="${!empty customLink}">
		<c:set var="linkUrl" value="${customLink}" scope="page"/>	
	</c:when>
	<c:otherwise>
		<c:set var="linkUrl" value="/group?who=${group.group.id}" scope="page"/>	
	</c:otherwise>
</c:choose>


<a href="${linkUrl}" style="text-decoration: none;"><dh:png src="${photoUrl}" style="width: ${size}; height: ${size};"/></a>
