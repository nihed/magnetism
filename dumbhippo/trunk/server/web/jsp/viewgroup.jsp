<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="fromInvite" param="fromInvite"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${viewgroup.name}"/></title>
	<dht:stylesheets href="group.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
        dojo.require("dh.util");
    </script>
</head>
<body>
	<dht:header><c:out value="${viewgroup.name}"/></dht:header>
	
	<dht:toolbar>
		<c:if test="${viewgroup.canShare}">
			 &#151; <a href="javascript:dh.util.openShareGroupWindow('${viewgroup.viewedGroupId}');">Share <c:out value="${viewgroup.name}"/> with friends</a>
		</c:if>
		<c:choose>
			<c:when test="${viewgroup.canLeave}">
				 &#151; <a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Leave <c:out value="${viewgroup.name}"/></a>
			</c:when>
			<c:when test="${viewgroup.canJoin}">
				 &#151; <a href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Join <c:out value="${viewgroup.name}"/></a>
			</c:when>
		</c:choose>
	</dht:toolbar>

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

	<div class="person">
		<dht:groupshot group="${viewgroup.viewedGroup}" size="128"/>
		<c:out value="${viewgroup.name}"/>
	</div>
	<c:if test="${viewgroup.canModify}">
		<div>
			<dht:uploadPhoto location="/groupshots" groupId="${viewgroup.viewedGroupId}" linkText="Change Group Photo"/>
		</div>
	</c:if>

	<div id="dhMain">
		<c:if test="${viewgroup.invitedNotAccepted}">
			<div>
				You were invited to this group by <dh:entity value="${viewgroup.inviter}" photo="true"/><br/>
				<a href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Accept invitation</a>&nbsp
				<a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Remove me</a>
			</div>
		</c:if>
			
		<table class="dh-main-table">
		<tr>
		<td class="dh-post-list-td">
			<div class="shared-links">	
				<strong>Cool New Links</strong>
				<dht:postList posts="${viewgroup.posts}" maxPosts="${viewgroup.maxPostsShown}" groupId="${viewgroup.viewedGroupId}" groupName="${viewgroup.name}"/>
			</div>
		</td>
		<td>
			<c:set var="invitedMembers" value="${viewgroup.invitedMembers}"/>
			<div class="group-members">
				<c:choose>
					<c:when test="${!empty invitedMembers}">
						<strong>Active members:</strong><br/>
						<dh:entityList value="${viewgroup.activeMembers}" photos="true"/><br/>
						<strong>Invited Members:</strong><br/>
						<dh:entityList value="${invitedMembers}" photos="true"/>
					</c:when>
					<c:otherwise>
						<strong>Members:</strong><br/>
						<dh:entityList value="${viewgroup.activeMembers}" photos="true"/>
					</c:otherwise>
				</c:choose>
			</div>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
