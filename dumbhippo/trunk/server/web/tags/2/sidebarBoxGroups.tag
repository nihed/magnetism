<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- If there are no groups for someone !self, just omit this box --%>
<c:if test="${person.groups.size > 0 || person.self}">
	
	<c:choose>
		<c:when test="${person.self}">
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
					<dht:moreLink moreName="ALL ${title}" more="/groups?who=${person.viewedUserId}"/>
				</c:if>
			</c:when>
			<c:otherwise> 
                 <c:if test="${person.invitedToFollowGroups.size == 0 && person.invitedGroups.size == 0}">
                     <p class="dh-sidebar-box-empty">Find a group to join,<br/>or start your own!</p>
                 </c:if>			
			</c:otherwise>
		</c:choose>
		
		<c:if test="${person.invitedToFollowGroups.size > 0 || person.invitedGroups.size > 0}">
		    <c:choose>
		        <c:when test="${person.newGroupInvites}">
                    <p><a class="dh-new-invited-groups" href="/groups?who=${person.viewedUserId}">New invited groups!</a></p>
                </c:when>
                <c:otherwise>
                    <p class="dh-sidebar-box-empty"><a href="/groups?who=${person.viewedUserId}">You still have group invitations</a></p>           
                </c:otherwise>
            </c:choose>
        </c:if>
          
     	<dht:sidebarBoxSeparator/>
		<dht:actionLink href="/groups?publiconly=true" title="Browse existing public groups">Browse public groups</dht:actionLink>
		<dht:actionLink href="/create-group" title="Create a new group of your very own">Create a new group</dht:actionLink>
	</dht:sidebarBox>
</c:if>
