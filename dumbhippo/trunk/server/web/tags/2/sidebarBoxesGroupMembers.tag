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
				<dht:moreLink more="/members?group=${group.viewedGroupId}"/>
			</c:if>
		</c:when>
		<c:otherwise>
			No members <%-- FIXME link to a place to add members --%>
		</c:otherwise>
	</c:choose>
	<dht:sidebarBoxSeparator/>
	<dht:actionLink href="FIXME" title="Create a new group of your very own">Create a group</dht:actionLink>
</dht:sidebarBox>

<c:if test="${group.invitedMembers.size > 0}">
	<dht:sidebarBox boxClass="dh-group-invited-members-box" title="${group.viewedGroup.liveGroup.invitedMemberCount} INVITED MEMBERS">
		<c:forEach items="${group.invitedMembers.list}" var="person">
			<dht:personItem who="${person}"/>
		</c:forEach>
		<c:if test="${group.viewedGroup.liveGroup.invitedMemberCount > group.maxMembersShown}">
			<dht:moreLink more="/members?group=${group.viewedGroupId}"/>
		</c:if>
	</dht:sidebarBox>
</c:if>
