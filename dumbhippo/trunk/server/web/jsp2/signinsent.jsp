<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="who" class="com.dumbhippo.web.WhoAreYouPage" scope="request"/>

<head>
	<title>Sign In</title>
	<link rel="stylesheet" type="text/css" href="/css2/who-are-you.css"/>
	<dht:scriptIncludes/>
</head>
<dht:body extraClass="dh-gray-background-page">
	<table style="width: 100%"><tr align="center"><td>
	<%-- Don't display logo when launched from the client to conserve space --%>
	<c:if test='${param["next"] == "close"}'>	
		<div id="dhHeaderLogo"><a href="/main"><img src="/images2/mugshot_logo.gif"/></a></div>	
	</c:if>
	<dht:zoneBox zone="login" topImage="/images2/header_login500.gif" bottomImage="/images2/bottom_gray500.gif" disableJumpTo="true">
		<form id="dhSigninForm" action="/signinpost" method="post">
			<table cellspacing="0px" cellpadding="0px" height="100%">
				<tr align="center">
				<td>
				<div class="dh-login-text">Sign-in link sent to <c:out value="${address}"/>; click on that link to sign in.</div>
				<c:if test='${param["next"] == "close"}'>					
					<p style="text-align: right">
						<input type="button" value="&nbsp;&nbsp;OK&nbsp;&nbsp;" onclick="window.close();"/>
					</p>
				</c:if>
				</td>
				</tr>
			</table>
		</form>
	</dht:zoneBox>
	<dht:footer/>	
	</td></tr></table>
</dht:body>
</html>
