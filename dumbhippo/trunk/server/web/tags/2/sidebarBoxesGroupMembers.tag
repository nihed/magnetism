<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dht:sidebarBox boxClass="dh-group-members-box" title="${group.viewedGroup.liveGroup.memberCount} MEMBERS">
	<c:choose>
		<c:when test="${group.activeMembers.size > 0}">
			<c:forEach items="${group.activeMembers.list}" end="${group.maxMembersShown - 1}" var="person">
				<dht:personItem who="${person}"/>
			</c:forEach>
			<c:if test="${group.viewedGroup.liveGroup.memberCount > group.maxMembersShown}">
				<dht:moreLink moreName="ALL MEMBERS" more="/members?group=${group.viewedGroupId}"/>
			</c:if>
		</c:when>
		<c:otherwise>
			<p class="dh-sidebar-box-empty">Nobody In This Group</p>
		</c:otherwise>
	</c:choose>
</dht:sidebarBox>

<c:if test="${group.followers.size > 0}">
	<dht:sidebarBox boxClass="dh-group-members-box" title="${group.viewedGroup.liveGroup.followerCount} FOLLOWERS">
		<c:forEach items="${group.followers.list}" end="${group.maxMembersShown - 1}" var="person">
			<dht:personFollowerItem group="${group}" who="${person}"/>
		</c:forEach>
		<c:if test="${group.viewedGroup.liveGroup.followerCount > group.maxMembersShown}">
			<dht:moreLink moreName="ALL FOLLOWERS" more="/members?group=${group.viewedGroupId}"/>
		</c:if>
	</dht:sidebarBox>
</c:if>

<c:if test="${group.member && group.invitedMembers.size > 0}"> <%-- Allow invitees to see other invitees FIXME having the "access control" check here is wrong, should be in GroupSystem --%>
	<dht:sidebarBox boxClass="dh-group-invited-members-box" title="${group.viewedGroup.liveGroup.invitedMemberCount} INVITED MEMBERS">
		<c:forEach items="${group.invitedMembers.list}" end="${group.maxMembersShown - 1}" var="person">
			<dht:personItem who="${person}"/>
		</c:forEach>
		<c:if test="${group.viewedGroup.liveGroup.invitedMemberCount > group.maxMembersShown}">
			<dht:moreLink moreName="ALL INVITED MEMBERS" more="/members?group=${group.viewedGroupId}"/>
		</c:if>
	</dht:sidebarBox>
</c:if>

<c:if test="${group.member && group.invitedFollowers.size > 0}"> <%-- Allow invitees to see other invitees FIXME having the "access control" check here is wrong, should be in GroupSystem --%>
	<dht:sidebarBox boxClass="dh-group-invited-members-box" title="${group.viewedGroup.liveGroup.invitedFollowerCount} INVITED FOLLOWERS">
		<c:forEach items="${group.invitedFollowers.list}" end="${group.maxMembersShown - 1}" var="person">
			<dht:personItem who="${person}"/>
		</c:forEach>
		<c:if test="${group.viewedGroup.liveGroup.invitedFollowerCount > group.maxMembersShown}">
			<dht:moreLink moreName="ALL INVITED FOLLOWERS" more="/members?group=${group.viewedGroupId}"/>
		</c:if>
	</dht:sidebarBox>
</c:if>
