<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean/>

<c:set var="pageName" value="Badges" scope="page"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht:faviconIncludes/>
</head>


<dht3:page>
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName}">
		<dht3:randomTip tipIndex="${person.randomTipIndex}" isSelf="${person.self}"/>
		<dht3:standardPageOptions selected="${pageName}"/>
	</dht3:pageSubHeader>
	
	<div>
		<dh:userSummary userId="${person.viewedPerson.viewPersonPageId}" height="74"/>
	</div>

	<div>&nbsp;</div>

	<div>
		<dh:userSummary userId="${person.viewedPerson.viewPersonPageId}" height="180"/>
	</div>

	<div>&nbsp;</div>

	<div>
		<dh:userSummary userId="${person.viewedPerson.viewPersonPageId}" height="255"/>
	</div>

	<div>&nbsp;</div>

	<div>
		<dh:nowPlaying userId="${person.viewedPerson.viewPersonPageId}"/>
	</div>

</dht3:page>

</html>
