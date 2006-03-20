<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="who" class="com.dumbhippo.web.WhoAreYouPage" scope="request"/>

<head>
	<title>Sign In</title>
	<dht:stylesheets href="small-box.css" />
</head>
<dht:bodySmallBox>
	<dht:smallBoxTopArea>
		<form id="dhSigninForm" action="/signinpost" method="post">
			<c:if test='${!empty param["next"]}'>
				<input type="hidden" value='${param["next"]}' name="next"/>
			</c:if>
			<table cellspacing="0px" cellpadding="0px">
				<tbody>
					<tr>
						<td>Email or AIM:</td>
						<td style="text-align: right;"><input type="text" class="dhText" name="address"/></td>
					</tr>
					<tr>
						<td colspan="2" style="text-align: center; padding-top:	7px; padding-bottom: 7px;">
							-
						</td>
					</tr>
					<tr>
						<td colspan="2" style="text-align: center;">
								<input type="submit" value="Send me a sign-in link" name="sendlink"/>
						</td>
					</tr>
					<tr>
						<td colspan="2" style="text-align: center;">
							- or -
						</td>
					</tr>
					<tr>
						<td>I might have a password:</td>
						<td style="text-align: right;"><input type="password" class="dhText" name="password"/></td>
					</tr>
					<tr>
						<td colspan="2" style="text-align: center;">								
							<input type="submit" value="Sign in with password" name="checkpassword"/>
						</td>
					</tr>
				</tbody>
			</table>
		</form>
	</dht:smallBoxTopArea>
	<c:if test="${param.wouldBePublic || !who.browser.supported}">
		<dht:smallBoxBottomArea>
			<c:if test="${param.wouldBePublic}">
				<dht:largeTitle>Don't have an account?</dht:largeTitle>
				<p>
					The page you're trying to get to will be public eventually, 
					but right now it's "members only." If you want in, leave us 
					your email address and we'll let you know when you can sign up.
				</p>
				<dht:wantsIn/>
			</c:if>
			<c:if test="${!who.browser.supported}">
				<dht:largeTitle>Your Web Browser</dht:largeTitle>
				Right now we've only tested the site with 
				<c:out value="${who.browser.supportedBrowsers}"/>. Your browser
				might work, but we haven't tried it ourselves.
				Feel free to give it a try and let us know if you find a problem.
				We'll test more browsers as soon as we can.
			</c:if>
		</dht:smallBoxBottomArea>
	</c:if>
</dht:bodySmallBox>
</html>
