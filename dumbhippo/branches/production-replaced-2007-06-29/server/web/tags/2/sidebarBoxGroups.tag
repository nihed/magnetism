<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- If there are no groups for someone !self, just omit this box --%>
<c:if test="${person.combinedGroups.size > 0 || person.self}">
	
	<c:choose>
		<c:when test="${person.self && !person.asOthersWouldSee}">
			<c:set var="title" value="YOUR GROUPS" scope="page"/>
		</c:when>
		<c:otherwise>
			<c:set var="title" value="GROUPS" scope="page"/>
		</c:otherwise>
	</c:choose>
	
	<dht:sidebarBox boxClass="dh-groups-box" title="${title}">
		<c:choose>
			<c:when test="${person.combinedGroups.size > 0}">
				<c:forEach items="${person.combinedGroups.list}" end="2" var="group">
					<dht:groupItem group="${group}"/>
				</c:forEach>
				<c:if test="${person.combinedGroups.size > 3}">
					<dht:moreLink moreName="ALL ${title} (${person.combinedGroups.size})" more="/groups?who=${person.viewedUserId}"/>
				</c:if>
			</c:when>
			<c:otherwise> 
                 <p class="dh-sidebar-box-empty">Find a group to join, or start your own!</p>
			</c:otherwise>
		</c:choose>
    
	    <c:if test="${person.invitedGroups.size > 0 || person.invitedToFollowGroups.size > 0}">
	        <dht:sidebarBoxSeparator/>
	        <c:choose>
	            <c:when test="${person.newGroupInvites}">
	                <dht:actionLink href="/group-invitations" title="You've been invited to new groups!">New invited groups! (${person.invitedGroups.size + person.invitedToFollowGroups.size})</dht:actionLink>
	            </c:when>
	            <c:otherwise>
	                <dht:actionLink href="/group-invitations" title="Groups you've been invited to">Invited groups (${person.invitedGroups.size + person.invitedToFollowGroups.size})</dht:actionLink>
	            </c:otherwise>
	        </c:choose>
	    </c:if>
        
     	<dht:sidebarBoxSeparator/>
		<dht:actionLink href="/public-groups" title="Browse existing public groups">Browse public groups</dht:actionLink>
		<dht:actionLink href="/active-groups" title="Browse recently active public groups">Browse active groups</dht:actionLink>
		<dht:actionLink href="/create-group" title="Create a new group of your very own">Create a new group</dht:actionLink>
	</dht:sidebarBox>
</c:if>
