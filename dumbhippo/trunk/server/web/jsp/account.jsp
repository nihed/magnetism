<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="account" class="com.dumbhippo.web.AccountPage" scope="request"/>

<head>
	<title>Your Account Setup</title>
	<dht:stylesheets href="account.css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header>
		Learn to share a link
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
				<!-- don't try to use css counters, firefox no like -->
				<span class="dh-step">1</span>
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
				<span class="dh-step">2</span>
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
				<span class="dh-step">3</span>
				Add an email address:
			</td>
			<td class="dh-edit-table-control">
				<input id="dhExtraEmailAddress"/>
				<input type="button" value="Send Verification"/>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">Click on the link you get in the mail.
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
