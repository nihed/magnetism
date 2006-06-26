<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test='${empty param["who"] && !signin.valid}'>
	<dht:errorPage>I don't know whose friends you are looking for!</dht:errorPage>
</c:if>

<c:choose>
	<c:when test='${empty param["who"]}'>
		<c:set var="fromHome" value='true' scope="page"/>
		<c:set var="who" value='${signin.user.id}' scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="fromHome" value='false' scope="page"/>
		<c:set var="who" value='${param["who"]}' scope="page"/>
	</c:otherwise>
</c:choose>

<dh:bean id="person" class="com.dumbhippo.web.pages.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>

<c:if test="${!person.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Friends</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxFriends back='true'>
			<dht:zoneBoxTitle>ALL <c:out value='${fromHome ? "YOUR " : "" }'/>FRIENDS</dht:zoneBoxTitle>
			<dht:twoColumnList>
				<c:forEach items="${person.contacts.list}" var="person">
					<dht:personItem who="${person}" invited="true"/>
				</c:forEach>
			</dht:twoColumnList>
		</dht:zoneBoxFriends>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
