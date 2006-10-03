<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.server.views.GroupView"%>
<%@ attribute name="size" required="false" type="java.lang.String" %>
<%@ attribute name="customLink" required="false" type="java.lang.String" %>

<c:if test="${empty size}">
	<c:set var="size" value="60"/>
</c:if>
<c:choose>
	<c:when test="${size == 60}">
		<c:set var="photoUrl" value="${group.group.photoUrl60}" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="photoUrl" value="/files/groupshots/${size}/${group.group.id}?v=${group.group.version}" scope="page"/>
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
