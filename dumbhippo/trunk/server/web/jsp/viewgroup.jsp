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
        <script type="text/javascript">
                dojo.require("dh.util");
        </script>
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
			<dht:uploadPhoto location="/groupshots" groupId="${viewgroup.viewedGroupId}"/>
	                <a id="dhChangeGroupPhotoLink" href="javascript:void(0);" onClick="dh.util.swapElements('dhPhotoUploadFileEntry','dhChangeGroupPhotoLink')">Change Group Photo</a>
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
				<c:forEach items="${viewgroup.posts}" var="post" varStatus="status">
					<dht:postBubble post="${post}"/>
					<c:if test="${status.last}">
		                            <div style="text-align:right"><input style="width:7em;border:1px solid black;" type="text" value="Search"/> the <a href="/viewgroup?groupId=${viewgroup.viewedGroupId}&skip=10">other shares</a> sent to the <c:out value="${viewgroup.name}"/> group.</div>
                                        </c:if>

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
