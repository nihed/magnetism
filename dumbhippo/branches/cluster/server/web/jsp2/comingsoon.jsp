<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>



<%-- Please don't bother hacking on this page much, it will only be used for another week or so --%>



<head>
	<title>Coming Soon!</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>	
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_blank500.gif">
	<div>
		This site is currently <b>SECRET</b> and <b>EXCLUSIVE</b>.
	</div>
	<br/>
	<div>
		If you have an account already, <b><a href="/who-are-you">please log in</a></b>.
	</div>
	<br/>	
	<div>
		If you're curious, leave your email address here, and we'll let you know when we launch.
		<br/>
		<dht:wantsIn/>		
	</div>
</dht:systemPage>
</html>
