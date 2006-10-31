<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="person" class="com.dumbhippo.web.pages.GroupsPage" scope="page"/>

<c:set var="pagetitle" value="All Public Groups" scope="page"/>

<head>
	<title><c:out value="${pagetitle}"/></title>
	<dht:siteStyle/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript" src="/javascript/${buildStamp}/dh/groups.js"></script>	
</head>
<dht:twoColumnPage>
    <c:if test="${signin.valid}">
	    <dht:sidebarPerson who="${signin.user.id}">
	    </dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxPublicGroups back='true'>
			<dht:zoneBoxTitle>
			    <c:out value="${fn:toUpperCase(pagetitle)}"/>
			</dht:zoneBoxTitle>
			<form action="/search" method="get">
				<div>
					Search:
					<jsp:element name="input">
						<jsp:attribute name="type">text</jsp:attribute>
						<jsp:attribute name="id">dhGroupsSearchEntry</jsp:attribute>
						<jsp:attribute name="class">dh-text-input</jsp:attribute>
						<jsp:attribute name="name">q</jsp:attribute>
					</jsp:element>
					<input type="submit" value="Go"/>
				</div>
			</form>
			<script type="text/javascript">
				dhGroupsSearchEntryInit = function () {
					var searchBox = document.getElementById('dhGroupsSearchEntry');
					var entry = new dh.textinput.Entry(searchBox, "Group topic or name", "");
				}
				dhGroupsSearchEntryInit()
			</script>
			<c:choose>
				<c:when test="${person.pageablePublicGroups.totalCount > 0}">
				    <dht:twoColumnList>
					<c:forEach items="${person.pageablePublicGroups.results}" var="group">
						<dht:groupItem group="${group}" controls="true"/>
					</c:forEach>    
					</dht:twoColumnList>
				    <dht:expandablePager pageable="${person.pageablePublicGroups}" anchor="dhPublicGroups"/>
				</c:when>
				<c:otherwise>
				    No public groups.
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxPublicGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>