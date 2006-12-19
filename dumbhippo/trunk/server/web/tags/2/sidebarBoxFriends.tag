<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- If there are no friends for someone !self, just omit this box --%>
<c:if test="${person.contacts.size > 0 || person.self}">

	<c:choose>
		<c:when test="${person.self && !person.asOthersWouldSee}">
            <c:set var="selfView" value='true' scope="page"/>
			<c:set var="title" value="YOUR FRIENDS" scope="page"/>
			<c:set var="url" value="/friends" scope="page"/>
		</c:when>
		<c:otherwise>
            <c:set var="selfView" value='false' scope="page"/>
			<c:set var="title" value="FRIENDS" scope="page"/>
			<c:set var="url" value="/friends?who=${person.viewedUserId}" scope="page"/>			
		</c:otherwise>
	</c:choose>
	
	<dht:sidebarBox boxClass="dh-friends-box" title="${title}">
		<c:choose>
			<c:when test="${person.contacts.size > 0}">
				<c:forEach items="${person.contacts.list}" end="2" var="person">
					<dht:personItem who="${person}"/>
				</c:forEach>
				<c:choose>				
				    <c:when test="${person.contacts.size > 3}">
					    <dht:moreLink moreName="ALL ${title} (${person.viewedPerson.liveUser.contactsCount})" more="${url}"/>
				    </c:when>
				    <c:when test="${(person.followers.size > 0) && selfView}">
					    <dht:moreLink moreName="ALL YOUR FOLLOWERS (${person.followers.size})" more="${url}"/>
				    </c:when>				    
				</c:choose>    
			</c:when>
			<c:otherwise>
			    <c:choose>
				    <c:when test="${person.signin.user.account.invitations > 0}">
					    <p class="dh-sidebar-box-empty">Email <a href="/invitation">invites</a> to some friends</p>
				    </c:when>
				    <c:otherwise>
					    <p class="dh-sidebar-box-empty">A loner huh?</p>
				    </c:otherwise>
			    </c:choose>
				<c:if test="${(person.followers.size > 0) && selfView}">
					<dht:moreLink moreName="ALL YOUR FOLLOWERS (${person.followers.size})" more="${url}"/>
				</c:if>
			</c:otherwise>
		</c:choose>
	    <dht:sidebarBoxSeparator/>
	    <c:choose>
		    <c:when test="${person.signin.user.account.invitations > 0}">
			    <dht:actionLink href="/invitation" title="Invite a friend">Invite a friend (${person.signin.user.account.invitations} invitations left)</dht:actionLink>
		    </c:when>
		    <c:when test="${person.outstandingInvitations.size > 0}">
                <dht:actionLink href="/invitation" title="Review pending invitations">Pending invitations (${person.outstandingInvitations.size} out there)</dht:actionLink>
		    </c:when>
		    <c:otherwise>
		        No invitations to extend to friends
		    </c:otherwise>  	
		</c:choose>    	    
		<dht:actionLink href="/active-people" title="Browse recently active people">Browse active people</dht:actionLink>
	</dht:sidebarBox>
</c:if>
