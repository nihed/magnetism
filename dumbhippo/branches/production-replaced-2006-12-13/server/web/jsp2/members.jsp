<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>
<jsp:setProperty name="group" property="allMembers" value="true"/>

<c:if test="${empty group.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${group.name}"/>'s Mugshot</title>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/group.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<dht:embedObject/>
</head>
<dht:twoColumnPage alwaysShowSidebar="true">
	<dht:sidebarGroup/>
	<dht:contentColumn>
		<dht:zoneBoxGroup back="/group?who=${group.viewedGroupId}">
			<dht:zoneBoxTitle>ALL GROUP MEMBERS</dht:zoneBoxTitle>
			<dht:twoColumnList>
				<c:forEach items="${group.pageableActiveMembers.results}" var="person">
					<dht:personItem who="${person}"/>
				</c:forEach>
			</dht:twoColumnList>
			<dht:expandablePager pageable="${group.pageableActiveMembers}" anchor="dhActiveMembers"/>
			
			<c:if test="${group.followers.size > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>ALL FOLLOWERS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${group.pageableFollowers.results}" var="person">
						<dht:personFollowerItem group="${group}" who="${person}"/>
					</c:forEach>
				</dht:twoColumnList>
				<dht:expandablePager pageable="${group.pageableFollowers}" anchor="dhFollowers"/>
			</c:if>

			<c:if test="${group.member && group.invitedMembers.size > 0}"> <%-- FIXME the access control check here is wrong, should be in GroupSystem --%>
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>ALL PENDING INVITATIONS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${group.pageableInvitedMembers.results}" var="person">
						<dht:personItem who="${person}"/>
					</c:forEach>
				</dht:twoColumnList>
				<dht:expandablePager pageable="${group.pageableInvitedMembers}" anchor="dhInvitedMembers"/>
			</c:if>
			
			<c:if test="${group.member && group.invitedFollowers.size > 0}"> <%-- FIXME access control check doesn't go here --%>
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>ALL INVITED FOLLOWERS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${group.pageableInvitedFollowers.results}" var="person">
						<dht:personItem who="${person}"/>
					</c:forEach>
				</dht:twoColumnList>
				<dht:expandablePager pageable="${group.pageableInvitedFollowers}" anchor="dhInvitedFollowers"/>
			</c:if>
			
		</dht:zoneBoxGroup>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
