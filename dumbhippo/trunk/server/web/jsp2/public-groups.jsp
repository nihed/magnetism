<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="person" class="com.dumbhippo.web.pages.GroupsPage" scope="page"/>

<c:set var="pagetitle" value="All Public Groups" scope="page"/>

<head>
	<title><c:out value="${pagetitle}"/></title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
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
			<c:choose>
				<c:when test="${person.allPublicGroups.size > 0}">
				    <dht:twoColumnList>
					<c:forEach items="${person.allPublicGroups.list}" var="group">
						<dht:groupItem group="${group}" controls="true"/>
					</c:forEach>    
					</dht:twoColumnList>
				</c:when>
				<c:otherwise>
				    No public groups.
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxPublicGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>