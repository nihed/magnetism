<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<c:choose>
		<c:when test="${signin.disabled}"> <%-- If the user goes to the URL directly; not normal --%>
			<title>Enable Account</title>
		</c:when>
		<c:otherwise>
			<title>Account Enabled</title>
		</c:otherwise>
	</c:choose>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_account500.gif">
	<c:choose>
		<c:when test="${signin.disabled}">
			<p>
				You have disabled your account. Reenable it to use all of Mugshot's features.
			</p>
			<div><a href="javascript:dh.actions.enableAccount()">Reenable my account</a></div>
			<div><a href="javascript:dh.actions.signOut()">Use Mugshot as a guest</a></div>
		</c:when>
		<c:otherwise>
			<p>
				Your account is now enabled.
			</p>
			<div><a href="javascript:dh.util.goToNextPage('/')">Continue</a></div>
		</c:otherwise>
	</c:choose>
</dht:systemPage>
</html>
