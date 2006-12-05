<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="activePeople" class="com.dumbhippo.web.pages.ActivePeoplePage" scope="request"/>

<head>
	<title><c:out value="Active People - Mugshot"/></title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="active-people">
	<dht3:pageSubHeader title="Active People">
	</dht3:pageSubHeader>
	
	<c:forEach items="${activePeople.activePeople.results}" var="person" varStatus="stackStatus">
		<dht3:personStack person="${person.personView}" stackOrder="${stackStatus.count + 1}" stackType="dhMugshot" blocks="${person.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${activePeople.activePeople}"/>
</dht3:page>
</html>