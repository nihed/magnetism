<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.GroupsPage"/>

<c:set var="pageName" value="Groups" scope="page"/>

<c:set var="followed" value="${param['followed']}" scope="page"/>
<c:choose>
	<c:when test="${empty followed}">
		<c:set var="displayedGroups" value="${person.activeGroups}" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="displayedGroups" value="${person.activeFollowedGroups}" scope="page"/>	
	</c:otherwise>
</c:choose>

<c:set var="possessive" value="${person.viewedPerson.name}'s" scope="page"/>
<c:set var="personIn" value="${person.viewedPerson.name} is in" scope="page"/>
<c:set var="personFollow" value="${person.viewedPerson.name} follows" scope="page"/>
<c:if test="${person.self}">
	<c:set var="possessive" value="My" scope="page"/>
	<c:set var="personIn" value="I'm in" scope="page"/>
	<c:set var="personFollow" value="I follow" scope="page"/>	
</c:if>

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
			<dht3:groupInvitedStack who="${group.groupView}" blocks="${group.blocks}" showFrom="true" stackOrder="1_${groupStatus.count}"/>
		</c:forEach>		
	</c:if>

	<div class="dh-page-title-container">
		<table cellspacing="0" cellpadding="0">
			<tr width="100%">
				<td width="30%"><span class="dh-page-title"><c:out value="${possessive}"/> ${pageName} (${person.activeAndFollowedGroupsCount})</span>
					<a class="dh-groups-create-link dh-underlined-link" href="/create-group">Create a Group</a>
				</td>
				<td align="right" width="70%"><div class="dh-page-options-container"><span class="dh-page-options"><dht3:personRelatedPagesTabs selected="groups"/></span></div></td>
			</tr>
		</table>
	</div>
	<div class="dh-page-options-sub-options-area dh-page-options">Show: 
	    <c:choose>
		     <c:when test="${empty followed}">
			     Groups <c:out value="${personIn}"/> |
				 <a href="/groups?who=${person.viewedPerson.viewPersonPageId}&followed=1">Groups <c:out value="${personFollow}"/></a>
			 </c:when>
		     <c:otherwise>
				<a href="groups?who=${person.viewedPerson.viewPersonPageId}">Groups <c:out value="${personIn}"/></a> |
				Groups <c:out value="${personFollow}"/>
			</c:otherwise>
	    </c:choose>
	</div>
	
	<c:forEach items="${displayedGroups.results}" var="group" varStatus="stackStatus">
	    <dht3:groupStack who="${group.groupView}" stackOrder="2_${stackStatus.count}" blocks="${group.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${displayedGroups}"/>
</dht3:page>
</html>
