<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="group" class="com.dumbhippo.web.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>
<jsp:setProperty name="group" property="allMembers" value="true"/>

<c:if test="${empty group.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${group.name}"/>'s Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
	<dht:scriptIncludes/>
	<dht:embedObject/>
    <script type="text/javascript">
        dojo.require("dh.util");
    </script>
</head>
<dht:twoColumnPage alwaysShowSidebar="true">
	<dht:sidebarGroup/>
	<dht:contentColumn>
		<dht:zoneBoxGroup back="/group?who=${group.viewedGroupId}">
			<dht:zoneBoxTitle>ALL GROUP MEMBERS</dht:zoneBoxTitle>
			<dht:twoColumnList>
				<c:forEach items="${group.activeMembers.list}" var="person">
					<dht:personItem who="${person}"/>
				</c:forEach>
			</dht:twoColumnList>
			
			<c:if test="${group.invitedMembers.size > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>ALL PENDING INVITATIONS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${group.invitedMembers.list}" var="person">
						<dht:personItem who="${person}"/>
					</c:forEach>
				</dht:twoColumnList>
			</c:if>
		</dht:zoneBoxGroup>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
