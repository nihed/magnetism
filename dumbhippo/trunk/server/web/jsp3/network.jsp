<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.MyFriendsPage"/>

<c:if test="${person.userContactCount == 0}">
	<jsp:forward page="/network-overview"/>
</c:if>

<html>
<%@ page pageEncoding="UTF-8" %>

<c:set var="pageName" value="Network" scope="page"/>
<c:set var="possessive" value="${person.viewedPerson.name}'s" scope="page"/>
<c:choose>
	<c:when test="${person.self}">
		<c:set var="possessive" value="My" scope="page"/>
		<c:set var="invitations" value="${person.viewedPerson.user.account.invitations}" scope="page"/>
		<c:if test="${invitations > 0}">
			<c:set var="titleLink" value="/invitation"/>
			<c:set var="titleLinkText" value="Invite friends (${invitations} invitations left)"/>
		</c:if>
	</c:when>
	<c:otherwise>
		<c:set var="invitations" value="0" scope="page"/>
	</c:otherwise>
</c:choose>

<head>
	<title><c:out value="${possessive}"/> ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht3:stylesheet name="person"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="network">
	<dht3:pageSubHeader title="${possessive} ${pageName} (${person.userContactCount})"
				titleLink="${titleLink}" titleLinkText="${titleLinkText}">
		<dht3:randomTip isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs selected="network"/>
	</dht3:pageSubHeader>
    <dht3:networkTabs selected="network-activity"/>
		
	<c:forEach items="${person.activePeople.results}" var="activePerson" varStatus="stackStatus">
		<dht3:personStack person="${activePerson.personView}" stackOrder="${stackStatus.count + 1}" stackType="dhMugshot" blocks="${activePerson.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${person.activePeople}"/>
</dht3:page>

</html>
