<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="activeGroups" class="com.dumbhippo.web.pages.ActiveGroupsPage" scope="request"/>

<head>
	<title><c:out value="Active Groups - Mugshot"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:faviconIncludes/>
</head>

<dht3:page>
	<dht3:pageSubHeader title="Active Groups">
	</dht3:pageSubHeader>
	
	<c:forEach items="${activeGroups.activeGroups.results}" var="group" varStatus="stackStatus">
	    <dht3:groupStack who="${group.groupView}" stackOrder="${stackStatus.count}" blocks="${group.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${activeGroups.activeGroups}"/>
</dht3:page>
</html>