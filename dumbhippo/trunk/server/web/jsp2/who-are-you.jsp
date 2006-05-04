<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Log In</title>
	<link rel="stylesheet" type="text/css" href="/css2/who-are-you.css"/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.login");
	</script>
</head>
<dht:body extraClass="dh-gray-background-page">
	<div id="dhHeaderLogo"><a href="/main"><img src="/images2/mugshot_logo.gif"/></a></div>	
	<dht:zoneBox zone="login" topImage="/images2/header_login500.gif" bottomImage="/images2/bottom_gray500.gif" disableJumpTo="true">
		<table cellspacing="0px" cellpadding="0px">
			<tr>
			<td>
			<form id="dhLoginNoPasswordForm">				
				<table id="dhLoginNoPassword" width="100%" valign="top" cellspacing="0px" cellpadding="0px">
					<tr><td class="dh-login-type-header">Log in without password</td></tr>
					<tr><td><div id="dhLoginSuccessful" class="dh-login-text" style="display: none;">Login link sent! Check your email or AIM.</div></td></tr>
					<tr><td class="dh-login-text">Email address or AIM:</td></tr>
					<tr><td><input type="text" name="address"/></td></tr>
					<tr><td><input id="dhSendLinkButton" type="submit" value="Send me a sign-in link" name="sendlink"
					               onclick="return dh.login.sendSigninLink();"/></td></tr>
				</table>
			</form>
			</td>
			<td width="1px" style="background: gray;"><div></div></td>
			<td>
			<form id="dhLoginWithPasswordForm" action="/signinpost" method="post">
				<c:if test='${!empty param["next"]}'>
					<input type="hidden" value='${param["next"]}' name="next"/>
				</c:if>				
				<table id="dhLoginWithPassword" width="100%" width="100%" valign="top" cellspacing="0px" cellpadding="0px">
					<tr><td class="dh-login-type-header">Log in with password</td></tr>
					<tr><td class="dh-login-text">Email address or AIM:</td></tr>					
					<tr><td><input type="text" class="dhText" name="address"/></td></tr>
					<tr><td class="dh-login-text">Password:</td></tr>
					<tr><td><input type="password" name="password" onkeypress="return dhPasswordKeyPress(event)"/></td></tr>	
					<tr><td><input id="dhPasswordButton" type="submit" value="Sign in with password" name="checkpassword"/></td></tr>
				</table>
			</td>
			</tr>
		</table>
	</dht:zoneBox>
	<dht:footer/>	
</dht:body>
</html>
