<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="fromInvite" param="fromInvite"/>
<jsp:setProperty name="group" property="viewedGroupId" param="who"/>

<c:if test="${empty group.viewedGroup}">
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
		<dht:zoneBoxGroup>
			<c:if test="${group.justAdded}">
				<div></div> <%-- IE bug workaround, display:none as first child causes problems --%>
				<div class="dh-notification">
					<c:choose>
						<c:when test="${group.member}">
							<div>You were invited to this group, but may <a href="javascript:dh.actions.leaveGroup('${group.viewedGroupId}')">Leave</a>
								at any time.</div>
						</c:when>
						<c:when test="${group.follower}">
							<div>You were invited to follow this group, but may <a href="javascript:dh.actions.leaveGroup('${group.viewedGroupId}')">stop following it</a>
								at any time.</div>
						</c:when>
					</c:choose>
				</div>
				<dht:zoneBoxSeparator/>
			</c:if>
			<c:if test="${group.latestTracks.resultCount > 0}">
				<dht:trackList name="RECENT GROUP SONGS" id="dhRecentSongs" tracks="${group.latestTracks.results}"
					pageable="${group.latestTracks}" separator="true" oneLine="true" displaySinglePersonMusicPlay="true"/>
			</c:if>
			<dht:zoneBoxTitle a="dhGroupPosts">LINKS RECENTLY SHARED WITH <c:out value="${fn:toUpperCase(group.name)}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${group.posts.resultCount > 0}">
					<dht:postList posts="${group.posts.results}" format="full-with-photos" favesMode="add-only"/>
					<dht:expandablePager pageable="${group.posts}" anchor="dhGroupPosts"/>
				</c:when>
				<c:otherwise>
					Nothing ever shared publicly with this group!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxGroup>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
