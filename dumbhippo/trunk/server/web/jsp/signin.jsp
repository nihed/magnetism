<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sign In</title>
	<dht:stylesheets href="signin.css" />
</head>
<body>
	<dht:header>
		Sign In
	</dht:header>
	<!--  no toolbar, it's weird on this page -->
	
	<div id="dhMain">
		<form class="dh-signin-form" action="/signinpost" method="post">
			<table>
				<tr class="dh-signin-address">
					<td>Email or AIM:</td>
					<td style="text-align: right;"><input type="text" class="dhText" name="address"/></td>
				</tr>
				<tr class="dh-signin-actions">
					<td colspan="2">
						<div>
							<input type="submit" value="Send me a sign-in link" name="sendlink"/>
						</div>
						<div>
							OR
						</div>
						<div>
							I have a <a href="/account">password</a>:
							<input type="text" class="dhText" name="password"/>
							<input type="submit" value="Sign in" name="checkpassword"/>
						</div>
					</td>
				</tr>
			</table>
		</form>
	</div>
</body>
</html>
