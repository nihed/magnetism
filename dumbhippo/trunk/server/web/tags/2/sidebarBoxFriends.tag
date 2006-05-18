<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- If there are no friends for someone !self, just omit this box --%>
<c:if test="${person.contacts.size > 0 || person.self}">

	<c:choose>
		<c:when test="${person.self}">
			<c:set var="title" value="MY FRIENDS" scope="page"/>
		</c:when>
		<c:otherwise>
			<c:set var="title" value="FRIENDS" scope="page"/>
		</c:otherwise>
	</c:choose>
	
	<dht:sidebarBox boxClass="dh-friends-box" title="${title}">
		<c:choose>
			<c:when test="${person.contacts.size > 0}">
				<c:forEach items="${person.contacts.list}" end="2" var="person">
					<dht:personItem who="${person}" invited="true"/>
				</c:forEach>
				<c:if test="${person.contacts.size > 3}">
					<dht:moreLink moreName="ALL FRIENDS" more="/friends?who=${person.viewedUserId}"/>
				</c:if>
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
			</c:otherwise>
		</c:choose>
		<c:if test="${person.signin.user.account.invitations > 0}">
			<dht:sidebarBoxSeparator/>
			<dht:actionLink href="/invitation" title="Invite a friend">Invite a friend (${person.signin.user.account.invitations} invitations left)</dht:actionLink>
		</c:if>
	</dht:sidebarBox>
</c:if>
