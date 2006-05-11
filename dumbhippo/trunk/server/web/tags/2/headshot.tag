<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="person" required="false" type="com.dumbhippo.server.PersonView"%>
<%@ attribute name="user" required="false" type="com.dumbhippo.persistence.User"%>
<%@ attribute name="size" required="false" type="java.lang.Integer" %>
<%@ attribute name="invited" required="false" type="java.lang.Boolean" %>

<c:if test="${empty user && !empty person}">
	<c:set var="user" value="${person.user}" scope="page"/>
</c:if>

<c:if test="${empty size}">
	<c:set var="size" value="60" scope="page"/>
</c:if>

<c:choose>
<c:when test="${!empty user}">
	<c:choose>
		<c:when test="${size == 60}">
			<c:set var="photoUrl" value="${user.photoUrl60}" scope="page"/>
		</c:when>
		<c:otherwise>
			<c:set var="photoUrl" value="/files/headshots/${size}/${user.id}?v=${user.version}" scope="page"/>
		</c:otherwise>
	</c:choose>
</c:when>
<c:when test="${invited}">
	<c:set var="photoUrl" value="/images2/invited60x60.gif" scope="page"/>
</c:when>
</c:choose>

<c:if test="${!empty photoUrl}">
	<a href="/person?who=${user.id}" style="text-decoration: none;"><dh:png src="${photoUrl}" style="width: ${size}; height: ${size};"/></a>
</c:if>
