<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Log In</title>
	<link rel="stylesheet" type="text/css" href="/css2/who-are-you.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.login");
	</script>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/header_login500.gif">
	<table cellspacing="0px" cellpadding="0px">
		<tr valign="top">
		<td width="240px" align="center">
		<form id="dhLoginNoPasswordForm" name="dhLoginNoPasswordForm" action="/signinpost" method="post">				
			<table id="dhLoginNoPassword" width="100%" cellspacing="0px" cellpadding="0px">
				<tr><td class="dh-login-type-header">Log in without password</td></tr>
				<tr><td><div id="dhLoginNotification" class="dh-login-text" style="display: none;"></div></td></tr>
				<tr><td class="dh-login-text">Email address or AIM:</td></tr>
				<tr><td><input type="text" class="dh-text-input" name="address"/></td></tr>
				<tr><td><input id="dhSendLinkButton" type="submit" value="Send me a sign-in link" name="sendlink"
				               onclick="return dh.login.sendSigninLink();"/></td></tr>
			</table>
		</form>
		</td>
		<td width="1px" style="background: gray;"><div></div></td>
		<td width="240px" align="center">
		<form id="dhLoginWithPasswordForm" name="dhLoginWithPasswordForm" action="/signinpost" method="post">
			<c:if test='${!empty param["next"]}'>
				<input type="hidden" value='${param["next"]}' name="next"/>
			</c:if>				
			<table id="dhLoginWithPassword" width="100%" cellspacing="0px" cellpadding="0px">
				<tr><td class="dh-login-type-header">Log in with password</td></tr>
				<tr><td class="dh-login-text">Email address or AIM:</td></tr>					
				<tr><td><input type="text" class="dh-text-input" name="address"/></td></tr>
				<tr><td class="dh-login-text">Password:</td></tr>
				<tr><td><input type="password" name="password"/></td></tr>	
				<tr><td><input id="dhPasswordButton" type="submit" value="Sign in with password" name="checkpassword"/></td></tr>
			</table>
		</form>
		</td>
		</tr>
	</table>
</dht:systemPage>	
</html>
