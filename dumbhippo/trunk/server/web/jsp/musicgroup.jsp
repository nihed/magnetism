<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.GroupMusicPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="fromInvite" param="fromInvite"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="who"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${viewgroup.name}'s Music"/></title>
	<dht:stylesheets href="musicgroup.css" iehref="group-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
        dojo.require("dh.util");
    </script>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<c:choose>
			<c:when test="${viewgroup.justAdded}">
				<div id="dhInformationBar"><dh:entity value="${viewgroup.inviter}" photo="true"/><p> invited you to this group, you can <a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>leave</a> it any time.</p></div>
			</c:when>
			<c:otherwise>

			</c:otherwise>
		</c:choose>

		<c:choose>
			<c:when test="${viewgroup.signin.disabled}">
				<%-- note this message appears even when viewing other people's pages --%>
				Your account is disabled; <a href="javascript:dh.actions.setAccountDisabled(false);">enable it again</a>
				to share stuff with friends.
			</c:when>
			<c:when test="${!viewgroup.signin.musicSharingEnabled}">
				<%-- again, we're using viewperson.signin, so appears even for others' pages --%>
				You haven't turned on music sharing. Turn it on to see what your friends in
				<c:out value="${viewgroup.name}"/> are listening 
				to lately, and share your music with them. 
					<a href="javascript:dh.actions.setMusicSharingEnabled(true);">Click here to turn it on</a>
			</c:when>
			<c:otherwise>
				<h2 class="dh-title"><c:out value="${viewgroup.name}"/>'s Recent Songs</h2>
		
				<div>
					<c:forEach items="${viewgroup.latestTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
			
				<h2 class="dh-title"><c:out value="${viewgroup.name}"/>'s Most Played Songs</h2>
		
				<div>
					<c:forEach items="${viewgroup.frequentTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
			</c:otherwise>
		</c:choose>
	</dht:mainArea>

	<dht:sidebarArea>
		<dht:sidebarAreaHeader>
			<dht:groupshot group="${viewgroup.viewedGroup}" size="192"/>
			<dht:sidebarAreaHeaderName value="${viewgroup.name}" canModify="false"/>
		</dht:sidebarAreaHeader>

		<dht:sidebarPanes>
			<dht:sidebarPane title="Group Members" last="true">
				<div class="dh-people">
					<dh:entityList value="${viewgroup.activeMembers}" photos="true" music="true"/>
				</div>
			</dht:sidebarPane>
		</dht:sidebarPanes>
	</dht:sidebarArea>

</dht:bodyWithAds>
</html>
