<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="invites" class="com.dumbhippo.web.InvitesPage" scope="request"/>
<jsp:setProperty name="invites" property="invitationToDelete" param="invitationToDelete"/>
<jsp:setProperty name="invites" property="invitationToRestore" param="invitationToRestore"/>
<jsp:setProperty name="invites" property="start" param="start"/>

<head>
	<title>Upgrade DumbHippo!</title>
	<dht:stylesheets/>
	<dht:scriptIncludes/>
</head>
<body>
<div id="dhContainer">

	<div id="dhMainArea">
	<dht:logo/>
	<h2>A new version of DumbHippo is ready to install!</h2>
	<i>Version 1.1.28:</i>
	<p>First release to the world!</p>
	<input type="button" value="Start using the new stuff!" onclick="window.external.application.DoUpgrade(); window.close();"/> 
	<input type="button" value="No, not right now." onclick="window.close();"/>
</div>

</body>
</html>