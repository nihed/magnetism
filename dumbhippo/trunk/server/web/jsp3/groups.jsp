<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.GroupsPage"/>

<c:set var="pageName" value="Groups" scope="page"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht3:stylesheet name="person"/>
	<dht3:stylesheet name="groups"/>
	<dh:script module="dh.groups"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="groups">	
	<c:if test="${person.self && fn:length(person.invitedGroupMugshots) > 0}">
		<div class="dh-groups-invited-header dh-page-title-container">
			You've been invited to join <dht3:plural n="${fn:length(person.invitedGroupMugshots)}" s="group"/>  
			<div class="dh-page-options"><c:if test="${fn:length(person.invitedGroupMugshots) > 2}"><a class="dh-underlined-link" href="/group-invitations">MORE</a></c:if></div>
		</div>
		<c:forEach items="${person.invitedGroupMugshots}" var="group" varStatus="groupStatus" end="1">
			<dht3:groupInvitedStack who="${group.groupView}" blocks="${group.blocks}" showFrom="true" stackOrder="1"/>
		</c:forEach>		
	</c:if>

	<div class="dh-page-title-container">
		<span class="dh-page-title"><c:choose><c:when test="${person.self}">My</c:when><c:otherwise><c:out value="${person.viewedPerson.name}"/>'s</c:otherwise></c:choose> ${pageName} (${person.activeGroups.totalCount})</span>
		<a class="dh-groups-create-link dh-underlined-link" href="/create-group">Create a Group</a>
		<div class="dh-page-options">
			<dht3:personRelatedPagesTabs selected="groups"/> 
		</div>
	</div>
	
	<c:forEach items="${person.activeGroups.results}" var="group" varStatus="stackStatus">
	    <dht3:groupStack who="${group.groupView}" stackOrder="2" blocks="${group.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${person.activeGroups}"/>
</dht3:page>
</html>
