<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${viewgroup.name}"/></title>
	<dht:stylesheets href="/css/group.css" />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header><c:out value="${viewgroup.name}"/></dht:header>
	
	<dht:toolbar>
		<c:if test="${empty viewgroup.inviter}">
			<c:if test="${viewgroup.isMember && !viewgroup.isForum}">
				<c:url var="addmembersurl" value="sharegroup?groupId=${viewgroup.viewedGroupId}"/>
				 &#151; <a href='${addmembersurl}'>Share <c:out value="${viewgroup.name}"/> with friends</a>
			</c:if>
			<c:choose>
				<c:when test="${viewgroup.isMember && empty viewgroup.inviter}">
					 &#151; <a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Leave <c:out value="${viewgroup.name}"/></a>
				</c:when>
				<c:when test="${viewgroup.canJoin}">
					 &#151; <a href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Join <c:out value="${viewgroup.name}"/></a>
				</c:when>
			</c:choose>
		</c:if>
	</dht:toolbar>

	<div class="person">
		<dht:png klass="cool-person" src="/files/groupshots/${viewgroup.viewedGroupId}" />
		<c:out value="${viewgroup.name}"/>
	</div>
	<c:if test="${viewgroup.canModify}">
		<div>
			<a href="/groupphoto?groupId=${viewgroup.viewedGroupId}">Change group photo</a>
		</div>
	</c:if>

	<div id="dhMain">
		<c:if test="${!empty viewgroup.inviter}">
			<div>
				You were invited to this group by <dh:entity value="${viewgroup.inviter}"/><br/>
				<a href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Accept invitation</a>&nbsp
				<a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Remove me</a>
			</div>
		</c:if>
			
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Cool New Links</strong>
				<c:forEach items="${viewgroup.posts}" var="post">
					<dht:postBubble post="${post}"/>
				</c:forEach>
			</div>
		</td>
		<td>
			<c:set var="invitedMembers" value="${viewgroup.invitedMembers}"/>
			<div class="group-members">
				<c:choose>
					<c:when test="${!empty invitedMembers}">
						<strong>Active members:</strong><br/>
						<dh:entityList value="${viewgroup.activeMembers}"/><br/>
						<strong>Invited Members:</strong><br/>
						<dh:entityList value="${invitedMembers}"/>
					</c:when>
					<c:otherwise>
						<strong>Members:</strong><br/>
						<dh:entityList value="${viewgroup.activeMembers}"/>
					</c:otherwise>
				</c:choose>
			</div>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
