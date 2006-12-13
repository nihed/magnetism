<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.MyFriendsPage"/>

<c:set var="pageName" value="Friends" scope="page"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="friends">
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName}">
		<dht3:randomTip tipIndex="${person.randomTipIndex}" isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs selected="friends"/>
	</dht3:pageSubHeader>

	<c:forEach items="${person.activePeople.results}" var="activePerson" varStatus="stackStatus">
		<dht3:personStack person="${activePerson.personView}" stackOrder="${stackStatus.count + 1}" stackType="dhMugshot" blocks="${activePerson.blocks}" showFrom="true"/>
	</c:forEach>
    <dht:expandablePager pageable="${person.activePeople}"/>
 
 	<c:if test="${person.activePeople.position != 0}">
	    <div class="dh-back">
	        <a href="/person?who=${person.viewedPerson.viewPersonPageId}">Back to <c:out value="${person.viewedPerson.name}"/>'s Home</a>
	    </div>
	</c:if>
</dht3:page>

</html>
