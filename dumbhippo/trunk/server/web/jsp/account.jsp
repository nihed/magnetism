<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="account" class="com.dumbhippo.web.AccountPage" scope="request"/>

<c:if test="${!account.signin.valid}">
	<!-- this is a bad error message but should never happen since we require signin to get here -->
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Your Account Setup</title>
	<dht:stylesheets href="account.css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header>
		Account Setup
	</dht:header>

	<dht:toolbar/>

	<div id="dhMain">
	
		<div class="dh-account-movie-area">
			<div class="dh-account-movie">account Movie Here</div>
		</div>
	
		<div class="dhBackgroundBox">
			<b>Help your friends find you</b>
			
			<table class="dh-edit-table">
			<tr>
			<td class="dh-edit-table-label">
				Your name is:
			</td>
			<td class="dh-edit-table-control">
				<dht:userNameEdit value="${account.person.name}"/>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">Click on the name to change it.
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
				<a href="${account.addAimLink}">
				IM us your screen name
				</a>
			</td>
			<td>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">Will not be seen by 
				strangers or used for spam.
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
				Add an email address:
			</td>
			<td class="dh-edit-table-control">
				<input id="dhExtraEmailAddress" class="dhText"/>
				<input type="button" value="Send Verification"/>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">Click on the link you get in the mail.
			</td>
			</tr>
			</table>
		</div>

		<br/>

		<div class="dhBackgroundBox">
			<b>Security and privacy</b>
			
			<table class="dh-edit-table">
			<tr>
			<td class="dh-edit-table-label">
				Set a password:
			</td>
			<td class="dh-edit-table-control">
				<input type="password" class="dhText"/>
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
				Type the same password again:
			</td>
			<td class="dh-edit-table-control">
				<input type="password" class="dhText"/>
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
			</td>
			<td class="dh-edit-table-control">
				<input type="button" value="Set Password"/>
			</td>
			</tr>			
			<tr>
			<td colspan="2" class="dh-explanation">You can email or IM yourself a sign-in link at any time, so a password is optional.
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
				<c:choose>
					<c:when test="${account.disabled}">
						<a href="javascript:dh.actions.setAccountDisabled(false);">Enable Account</a>
					</c:when>
					<c:otherwise>
						<a href="javascript:dh.actions.setAccountDisabled(true);">Disable Account</a>
					</c:otherwise>
				</c:choose>
			</td>
			<td>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">
				<c:choose>
					<c:when test="${account.disabled}">
						Enabling your account will give you a public page and let you 
						share links and photos with other people.
					</c:when>
					<c:otherwise>
						Disabling your account means you have no 
						public page and we will never send you email for any reason.			
					</c:otherwise>
				</c:choose>
				<a href="/privacy" target="_blank">Our privacy policy</a>
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
				<a href="javascript:dh.actions.signOut();">Sign Out</a>
			</td>
			<td>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">Sign out to keep other people on this computer from using your account.
			</td>
			</tr>
			</table>
		</div>
		
		<p>
		All done? <a href="home" target="_top">Return to your page.</a>
		</p>
		
	</div>
	
</body>
</html>
