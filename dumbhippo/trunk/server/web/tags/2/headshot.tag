<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.EntityView"%>
<%@ attribute name="size" required="false" type="java.lang.Integer" %>
<%@ attribute name="invited" required="false" type="java.lang.Boolean" %>
<%@ attribute name="customLink" required="false" type="java.lang.String" %>

<c:if test="${empty size}">
	<c:set var="size" value="60" scope="page"/>
</c:if>

<c:choose>
	<c:when test="${!empty customLink}">
		<c:set var="linkUrl" value="${customLink}"/>
	</c:when>
	<c:otherwise>
		<c:set var="linkUrl" value="${person.homeUrl}"/>
	</c:otherwise>
</c:choose>
<c:set var="photoUrl" value="${person.smallPhotoUrl}"/>  <%-- Only handles size=60 --%>
<c:choose>
	<c:when test="${!empty linkUrl && !empty photoUrl}">
		<a href="${linkUrl}" style="text-decoration: none;" target="_top"><dh:png src="${photoUrl}" style="width: ${size}; height: ${size}; border: none;"/></a>
	</c:when>
	<c:when test="${!empty photoUrl}">
		<dh:png src="${photoUrl}" style="width: ${size}; height: ${size}; border: none;"/>
	</c:when>
</c:choose>
