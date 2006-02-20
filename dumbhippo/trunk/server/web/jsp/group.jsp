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
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

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
					You were invited to this "<c:out value="${viewgroup.name}"/>" group
					by <dh:entity value="${viewgroup.inviter}" photo="true"/>.
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

		<h2 class="dh-title">Links This Group Shared</h2>

		<div id="dhSharesArea">
			<dht:postList posts="${viewgroup.posts}" maxPosts="${viewgroup.maxPostsShown}" groupId="${viewgroup.viewedGroupId}" groupName="${viewgroup.name}"/>
		</div>
	</div>

	<div id="dhPersonalArea">
		<div id="dhPhotoNameArea">
		<dht:groupshot group="${viewgroup.viewedGroup}" size="192"/>
		<c:if test="${viewgroup.canModify}">
			<dht:uploadPhoto location="/groupshots" groupId="${viewgroup.viewedGroupId}" linkText="change group photo" reloadTo="/group?who=${viewgroup.viewedGroupId}"/>
		</c:if>
		<div id="dhName"><c:out value="${viewgroup.name}"/></div>
		</div>

		<div class="dh-right-box-area">
		<c:set var="invitedMembers" value="${viewgroup.invitedMembers}"/>
		<c:choose>
			<c:when test="${!empty invitedMembers}">
			<div class="dh-right-box">
				<h5 class="dh-title">Active Group Members</h5>
				<div class="dh-people">
					<dh:entityList value="${viewgroup.activeMembers}" photos="true"/><br/>
				</div>
			</div>
			<div class="dh-right-box dh-right-box-last">
				<h5 class="dh-title">Invited Group Members</h5>
				<p class="dh-right-box-text">What Are <a class="dh-invited-group-members" href="http://info.dumbhippo.com/Invited Group Members">Invited Group Members</a>?</p>
				<div class="dh-people">
					<dh:entityList value="${invitedMembers}" photos="true"/>
				</div>
			</div>
			</c:when>
			<c:otherwise>
			<div class="dh-right-box dh-right-box-last">
				<h5 class="dh-title">Group Members</h5>
				<div class="dh-people">
					<dh:entityList value="${viewgroup.activeMembers}" photos="true"/>
				</div>
			</div>
			</c:otherwise>
		</c:choose>
		</div>
	</div>
	<dht:bottom/>
</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>
