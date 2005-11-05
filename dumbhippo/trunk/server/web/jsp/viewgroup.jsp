<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<c:if test="${empty viewgroup.viewedGroupId}"><jsp:forward page="/jsp/nogroup.jsp"/></c:if>

<head>
	<title><c:out value="${viewgroup.name}"/></title>
	<link rel="stylesheet" href="/css/group.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header><c:out value="${viewgroup.name}"/></dht:header>
	
	<dht:toolbar>
		<c:choose>
			<c:when test="${viewgroup.isMember && empty viewgroup.inviter}">
				 &#151; <a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Leave <c:out value="${viewgroup.name}"/></a>
			</c:when>
			<c:when test="${viewgroup.canJoin}">
				 &#151; <a href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Join <c:out value="${viewgroup.name}"/></a>
			</c:when>
		</c:choose>
	</dht:toolbar>

	<div class="main">
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
