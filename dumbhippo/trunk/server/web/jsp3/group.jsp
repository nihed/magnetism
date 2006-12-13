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
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="group">
	<dht3:pageSubHeader title="${group.private ? 'Private Group' : 'Public Group'}" privatePage="${group.private}">
		<dht3:groupRelatedPagesTabs group="${group.viewedGroup}" selected="group"/>
	</dht3:pageSubHeader>
    <dht3:groupStack who="${group.viewedGroup}" stackOrder="1" stackType="dhMugshot" pageable="${group.pageableMugshot}" shortVersion="${group.pageableStack.position > 0}" showFrom="true" showHomeUrl="false" />
	<dht3:shinyBox color="grey">
	    <div class="dh-person-stacker-header">
		    <span class="dh-person-header-name"><a name="dhStacker">Group Stacker</a></span>
		    <span class="dh-person-header-description">Updates from group members and feeds</span>
		</div>    
		<c:choose>
			<c:when test="${group.pageableMugshot.position == 0}">
				<dht3:stacker stackOrder="2" stackType="dhStacker" pageable="${group.pageableStack}" showFrom="true"/>
			</c:when>
			<c:otherwise>
			    <div class="dh-back">
		    	    <a href="/group?who=${group.viewedGroupId}">Back to <c:out value="${group.viewedGroup.name}"/>'s Home</a>
			    </div>
		    </c:otherwise>
		</c:choose>
	</dht3:shinyBox>	
</dht3:page>
</html>
