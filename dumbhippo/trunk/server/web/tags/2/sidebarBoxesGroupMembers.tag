<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dht:sidebarBox boxClass="dh-group-members-box" title="NN GROUP MEMBERS">
	<c:choose>
		<c:when test="${group.activeMembers.size > 0}">
			<c:forEach items="${group.activeMembers.list}" var="person">
				<dht:sidebarBoxPersonItem who="${person}"/>
			</c:forEach>
		</c:when>
		<c:otherwise>
			No members <%-- FIXME link to a place to add members --%>
		</c:otherwise>
	</c:choose>
	<dht:moreLink more="/group-members"/>
	<dht:sidebarBoxSeparator/>
	<c:if test="${group.canShare}">
		<dht:actionLink title="Share ${group.nameAsHtml} with friends" href="javascript:dh.util.openShareGroupWindow('${group.viewedGroupId}');">Invite new members</dht:actionLink>
	</c:if>	
	<dht:actionLink href="FIXME" title="Create a new group of your very own">Create a group</dht:actionLink>
</dht:sidebarBox>

<c:if test="${group.invitedMembers.size > 0}">
	<dht:sidebarBox boxClass="dh-group-invited-members-box" title="NN INVITED MEMBERS" more="/group-members">
		<c:forEach items="${group.invitedMembers.list}" var="person">
			<dht:sidebarBoxPersonItem who="${person}"/>
		</c:forEach>
	</dht:sidebarBox>
</c:if>
