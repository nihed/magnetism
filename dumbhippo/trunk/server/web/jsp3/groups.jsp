<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.MyGroupsPage"/>

<c:set var="pageName" value="Groups" scope="page"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht3:stylesheet name="person"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="groups">	
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName} (${person.activeGroups.totalCount})">
		<dht3:randomTip tipIndex="${person.randomTipIndex}" isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs selected="groups"/> 
	</dht3:pageSubHeader>
	
	<c:forEach items="${person.activeGroups.results}" var="group" varStatus="stackStatus">
	    <dht3:groupStack who="${group.groupView}" stackOrder="${stackStatus.count}" blocks="${group.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${person.activeGroups}"/>
</dht3:page>
</html>
