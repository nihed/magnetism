<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- If there are no friends for someone !self, just omit this box --%>
<c:if test="${sidebar.contacts.size > 0 || sidebar.self}">

	<c:choose>
		<c:when test="${sidebar.self}">
			<c:set var="title" value="MY FRIENDS" scope="page"/>
		</c:when>
		<c:otherwise>
			<c:set var="title" value="FRIENDS" scope="page"/>
		</c:otherwise>
	</c:choose>
	
	<dht:sidebarBox boxClass="dh-friends-box" title="${title}" more="/friends">
		<c:choose>
			<c:when test="${sidebar.contacts.size > 0}">
				<c:forEach items="${sidebar.contacts.list}" var="person">
					<dht:personItem who="${person}"/>
				</c:forEach>
			</c:when>
			<c:otherwise>
				No friends <%-- FIXME link to a place to add friends --%>
			</c:otherwise>
		</c:choose>
	</dht:sidebarBox>
</c:if>
