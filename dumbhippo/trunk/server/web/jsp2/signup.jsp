<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Sign Up for Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_signup500.gif" fullHeader="true">

	<p><strong>Enter your email address, and when we are ready to open our doors we will send you an invitation link!</strong></p>

    <dht:wantsIn buttonText="Sign Me Up!"/>

	<dht:zoneBoxSeparator/>

	<dht:notevil ownSection="true"/>
	
</dht:systemPage>
</html>
