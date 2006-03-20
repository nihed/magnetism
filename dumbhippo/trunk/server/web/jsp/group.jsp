<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="fromInvite" param="fromInvite"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="who"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${viewgroup.name}"/></title>
	<dht:stylesheets href="group.css" iehref="group-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
        dojo.require("dh.util");
    </script>
</head>
<dht:bodyWithAds>

	<dht:mainArea>
		<dht:toolbar account="false">
			<c:if test="${viewgroup.canShare}">
				 &#151; <a class="dh-toolbar-item" title="Share <c:out value="${viewgroup.name}"/> with friends" href="javascript:dh.util.openShareGroupWindow('${viewgroup.viewedGroupId}');">Share Group</a>
			</c:if>
			<c:choose>
				<c:when test="${viewgroup.canLeave}">
					 &#151; <a class="dh-toolbar-item" href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Leave Group</a>
				</c:when>
				<c:when test="${viewgroup.canJoin}">
					 &#151; <a class="dh-toolbar-item" href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Join This Group</a>
				</c:when>
			</c:choose>
		</dht:toolbar>
		<c:if test="${!viewgroup.fromInvite}"> <!-- we don't want to duplicate the download link -->
			<dht:infobar/>		
		</c:if>

		<c:choose>
			<c:when test="${viewgroup.justAdded}">
				<div id="dhInformationBar"><dh:entity value="${viewgroup.inviter}" photo="true"/><p> invited you to this group, you can <a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>leave</a> it any time.</p></div>
			</c:when>
			<c:otherwise>

			</c:otherwise>
		</c:choose>

		<c:if test="${viewgroup.fromInvite}">
			<div>
				<p>
					DumbHippo is a new way to share things with your friends.
				</p>
				<p> 
					<dh:entity value="${viewgroup.inviter}" photo="true"/> invited you to
					this "<c:out value="${viewgroup.name}"/>" group.
					<c:if test="${viewgroup.canLeave}">
						You can 
						<a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>leave this group at any time</a>.
					</c:if>
				</p>
				<p>
					<c:if test="${browser.windows}">
						DumbHippo is better with our easy Windows software, 
						<a href="${viewgroup.downloadUrlWindows}">click here to download it now</a>.
					</c:if>
				</p>
				<p>
					<a href="/welcome">Get the full details on DumbHippo</a>
				</p>
			</div>
		</c:if>
				
		<div class="dh-group-access"> 
		    <c:choose>
		        <c:when test="${viewgroup.private}">
		            This is a private group.
		        </c:when>
		        <c:otherwise>
		            This is a public group.
		        </c:otherwise>
		    </c:choose>
		</div>
		
		<dht:largeTitle>The <c:out value="${viewgroup.name}"/> Group</dht:largeTitle>

		<c:if test="${!empty viewgroup.latestTrack}">
			<h5 class="dh-title"><c:out value="${viewgroup.name}"/>'s Latest Song</h5>
			<dht:track track="${viewgroup.latestTrack}" linkifySong="false" playItLink="false"/>
			<div class="dh-more-songs"><a class="dh-more-songs" href="/musicgroup?who=${viewgroup.viewedGroupId}">More songs</a></div>
		</c:if>

		<h3 class="dh-title">Recent Links Shared with <c:out value="${viewgroup.name}"/></h3>

		<div id="dhSharesArea">
			<dht:postList posts="${viewgroup.posts}" maxPosts="${viewgroup.maxPostsShown}" groupId="${viewgroup.viewedGroupId}" groupName="${viewgroup.name}"/>
		</div>
	</dht:mainArea>

	<dht:sidebarArea>
		<dht:sidebarAreaHeader>
			<dht:groupshot group="${viewgroup.viewedGroup}" size="192"/>
			<c:if test="${viewgroup.canModify}">
				<dht:uploadPhoto location="/groupshots" groupId="${viewgroup.viewedGroupId}" linkText="change group photo" reloadTo="/group?who=${viewgroup.viewedGroupId}"/>
			</c:if>
			<dht:sidebarAreaHeaderName value="${viewgroup.name}" canModify="false"/>
		</dht:sidebarAreaHeader>

		<dht:sidebarPanes>
			<c:set var="invitedMembers" value="${viewgroup.invitedMembers}"/>
			<c:choose>
				<c:when test="${!empty invitedMembers}">
					<dht:sidebarPane title="Active Group Members">
						<div class="dh-people">
							<dh:entityList value="${viewgroup.activeMembers}" photos="true"/><br/>
						</div>
					</dht:sidebarPane>
					<dht:sidebarPane title="Invited Group Members" last="true">
						<!-- FIXME: create an info page on Invited Group Members and uncomment the next line -->
						<!-- <p class="dh-right-box-text">What Are <a class="dh-invited-group-members" href="http://info.dumbhippo.com/Invited Group Members">Invited Group Members</a>?</p> -->
						<div class="dh-people">
							<dh:entityList value="${invitedMembers}" photos="true"/>
						</div>
					</dht:sidebarPane>
				</c:when>
				<c:otherwise>
					<dht:sidebarPane title="Group Members" last="true">
						<div class="dh-people">
							<dh:entityList value="${viewgroup.activeMembers}" photos="true"/>
						</div>
					</dht:sidebarPane>
				</c:otherwise>
			</c:choose>
		</dht:sidebarPanes>
	</dht:sidebarArea>

</dht:bodyWithAds>
</html>
