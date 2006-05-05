<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="page" required="false" type="java.lang.String" %>

<%-- If there are no groups for someone !self, just omit this box --%>
<c:if test="${person.groups.size > 0 || person.self}">
	
	<c:choose>
		<c:when test="${person.self}">
			<c:set var="title" value="MY GROUPS" scope="page"/>
		</c:when>
		<c:otherwise>
			<c:set var="title" value="GROUPS" scope="page"/>
		</c:otherwise>
	</c:choose>
	
	<dht:sidebarBox boxClass="dh-groups-box" title="${title}">
		<c:choose>
			<c:when test="${person.groups.size > 0}">
				<c:forEach items="${person.groups.list}" end="2" var="group">
					<dht:groupItem group="${group}"/>
				</c:forEach>
				<c:if test="${person.groups.size > 3}">
					<c:choose>
						<c:when test='${page == "home"}'>
							<dht:moreLink more="/groups"/>
						</c:when>
						<c:otherwise>
							<dht:moreLink more="/groups?who=${person.viewedUserId}"/>
						</c:otherwise>
					</c:choose>
				</c:if>
			</c:when>
			<c:otherwise>
				No groups <%-- FIXME link to a place to add groups --%>
			</c:otherwise>
		</c:choose>
	</dht:sidebarBox>
</c:if>
