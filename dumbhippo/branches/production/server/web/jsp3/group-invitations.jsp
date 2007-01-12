<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.GroupsPage"/>

<c:set var="pageName" value="Group Invitations" scope="page"/>

<head>
	<title>My ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht3:stylesheet name="person"/>
	<dht3:stylesheet name="groups"/>
	<dh:script module="dh.groups"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="group-invitations">	
	<dht3:pageSubHeader title="My Group Invitations">
		<dht3:personRelatedPagesTabs/> 	
	</dht3:pageSubHeader>

	<c:forEach items="${person.invitedGroupMugshots}" var="group" varStatus="groupStatus">
		<dht3:groupInvitedStack who="${group.groupView}" blocks="${group.blocks}" showFrom="true" stackOrder="1"/>
	</c:forEach>		
</dht3:page>
</html>
