<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Oops!</title>
	<link rel="stylesheet" type="text/css" href="/css2/who-are-you.css"/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/header_login500.gif">
	<p>Ooops!  Our system burped.  The developers have been notified. </p>
	
	<p><a href='javascript:history.back();'>Go back</a>
	<c:if test='${!empty param["retry"]}'> or <a href='${param["retry"]}'>try again</a></c:if></p>
</dht:systemPage>	
</html>
