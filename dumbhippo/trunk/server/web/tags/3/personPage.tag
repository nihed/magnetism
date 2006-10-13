<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="pageName" required="true" type="java.lang.String" %>
<%@ attribute name="stackSize" required="false" type="java.lang.Integer" %>

<dht3:requirePersonBean/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:scriptIncludes/>
	<script src="/javascript/${buildStamp}/dh/stacker.js" type="text/javascript"></script>	
	<dht:faviconIncludes/>
</head>

<dht3:page>
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName}">
		<dht3:standardPageOptions selected="${pageName}"/>
	</dht3:pageSubHeader>
	
	<dht3:shinyBox color="grey">
		<dht3:personHeader who="${person.viewedPerson}" isSelf="true"><a href="/account">Edit my Mugshot account</a></dht3:personHeader>
		<dht3:stacker person="${person.viewedPerson}" stack="${person.participantOnlyStack}" stackSize="${stackSize}" stackOrder="1"/>
	</dht3:shinyBox>
	
	<jsp:doBody/>
</dht3:page>
