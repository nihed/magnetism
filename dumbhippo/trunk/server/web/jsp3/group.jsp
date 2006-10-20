<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="group" class="com.dumbhippo.web.pages.StackedGroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="who"/>

<c:if test="${empty group.viewedGroup}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${group.viewedGroup.name}"/> - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:scriptIncludes/>
	<script src="/javascript/${buildStamp}/dh/stacker.js" type="text/javascript"></script>	
	<dht:faviconIncludes/>
</head>

<dht3:page>
	<dht3:pageSubHeader title="${group.private ? 'Private Group' : 'Public Group'}" privatePage="${group.private}">
		<dht3:standardGroupOptions groupId="${group.viewedGroupId}" selected="Home"/>
	</dht3:pageSubHeader>
	<dht3:shinyBox color="grey">
		<dht3:groupHeader who="${group.viewedGroup}" shortVersion="${group.pageableStack.position > 0}">
			<c:choose>
				<%-- Be careful if changing this not to show both join and leave at the same time --%>
				<c:when test="${!empty group.joinAction}">
					 <a href="javascript:dh.actions.joinGroup('${group.viewedGroupId}')" title="${group.joinTooltip}"><c:out value="${group.joinAction}"/></a>
				</c:when>							
				<c:when test="${!empty group.leaveAction}">
					 <a href="javascript:dh.actions.leaveGroup('${group.viewedGroupId}')" title="${group.leaveTooltip}"><c:out value="${group.leaveAction}"/></a>
				</c:when>
			</c:choose>
			<c:if test="${group.canShare}">
				 | <a href="/group-invitation?group=${group.viewedGroupId}" title="Invite other people to this group">Invite People</a>
			</c:if>
		</dht3:groupHeader>
	    <dht3:stacker stackOrder="1" stackType="dhMugshot" pageable="${group.pageableStack}" showFrom="false"/>
	</dht3:shinyBox>
</dht3:page>
</html>