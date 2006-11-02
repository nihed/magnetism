<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<dht:errorPage>Group invitations only visible to signed in users!</dht:errorPage>
</c:if>

<c:set var="who" value='${signin.user.id}' scope="page"/>

<dh:bean id="person" class="com.dumbhippo.web.pages.GroupInvitationsPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>

<head>
	<title>Your Group Invitations</title>
	<dht:siteStyle/>	
	<dht:faviconIncludes/>
	<dh:script module="dh.groups"/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxGroupInvitations back='true'>
			<dht:zoneBoxTitle>
			    GROUPS YOU'VE BEEN INVITED TO JOIN
			</dht:zoneBoxTitle>	
			<c:choose>
				<c:when test="${person.invitedGroups.size > 0}">
				    <dht:twoColumnList>
					    <c:forEach items="${person.invitedGroups.list}" var="group">
						    <dht:groupItem group="${group}" controls="true"/>
					    </c:forEach>
					</dht:twoColumnList>
				</c:when>
				<c:otherwise>
				    No outstanding invitations to join groups.
				</c:otherwise>
			</c:choose>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>
			    GROUPS YOU'VE BEEN INVITED TO FOLLOW
			</dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${person.invitedToFollowGroups.size > 0}">
				    <dht:twoColumnList>
					    <c:forEach items="${person.invitedToFollowGroups.list}" var="group">
						    <dht:groupItem group="${group}" controls="true"/>
					    </c:forEach>
					</dht:twoColumnList>
				</c:when>
				<c:otherwise>
				    No outstanding invitations to follow groups.
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxGroupInvitations>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>