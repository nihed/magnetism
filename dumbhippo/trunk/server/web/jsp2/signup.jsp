<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Sign Up for Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
    <script type="text/javascript" src="/javascript/${buildStamp}/dh/wantsin.js"></script>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_signup500.gif" fullHeader="true">

	<p><strong>Enter your email address, and when we are ready to open our doors we will send you an invitation link!</strong></p>

	<form action="/wantsin" method="post" onsubmit="return dh.wantsin.send();">
	<table>
	     <tr><td>Email Address:</td><td><dht:textInput id="dhWantsInEmailEntry" name="address"/></td></tr>
	     <tr><td> </td>
	         <td style="text-align:right;"><input type="submit" id="dhWantsInSend" value="Sign Me Up!" /></td></tr>
	</table>
	</form>

	<dht:zoneBoxSeparator/>

	<dht:notevil ownSection="true"/>
	
</dht:systemPage>
</html>
