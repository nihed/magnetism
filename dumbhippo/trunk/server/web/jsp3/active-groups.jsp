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
	<dht:scriptIncludes/>
	<script src="/javascript/${buildStamp}/dh/stacker.js" type="text/javascript"></script>	
	<dht:faviconIncludes/>
</head>

<dht3:page>
	<dht3:pageSubHeader title="Active Groups">
	</dht3:pageSubHeader>
	
	<c:forEach items="${activeGroups.activeGroups.results}" var="group">
		<dht3:shinyBox color="orange">
			<dht3:groupHeader who="${group.groupView}">
				<%-- FIXME: show group actions here ... need to move stuff from StackedGroupPage to GroupView --%>
			</dht3:groupHeader>
		    <dht3:stacker stackOrder="1" stackType="dhMugshot" blocks="${group.blocks}" showFrom="false"/>
		</dht3:shinyBox>
	</c:forEach>
    <dht:expandablePager pageable="${activeGroups.activeGroups}"/>
</dht3:page>
</html>