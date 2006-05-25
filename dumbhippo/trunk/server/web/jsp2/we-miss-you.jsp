<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Log In</title>
	<link rel="stylesheet" type="text/css" href="/css2/who-are-you.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/header_account500.gif">
	<c:choose>
		<c:when test="${signin.disabled}">
			<p>
				You have previously disabled your account; in order to start using Mugshot
				again you'll need to reenable it.
			</p>
			<div><a href="javascript:dh.actions.enableAccount()">Reenable my account</a></div>
			<div><a href="javascript:dh.actions.signOut()">Use Mugshot logged out</a></div>
		</c:when>
		<c:otherwise> <%-- If the user goes to the URL directly; not normal --%>
			<p>
				Your account is now enabled.
			</p>
			<div><a href="javascript:dh.util.goToNextPage('/')">Continue</a></div>
		</c:otherwise>
	</c:choose>
</dht:systemPage>
</html>
