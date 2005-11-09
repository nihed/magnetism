<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="tutorial" class="com.dumbhippo.web.TutorialPage" scope="request"/>

<head>
	<title>How To Share a Link</title>
	<link rel="stylesheet" href="/css/tutorial.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header>
		Learn to share a link
	</dht:header>

	<dht:toolbar/>

	<div id="dhMain">
	
		<div class="dh-tutorial-movie-area">
			<div class="dh-tutorial-movie">Tutorial Movie Here</div>
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
				<dht:userNameEdit value="${tutorial.person.humanReadableName}"/>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="dh-explanation">Click on the name to change it.
			</td>
			</tr>
			<tr>
			<td class="dh-edit-table-label">
				<span class="dh-step">2</span>
				<a href="aim:GoIM?screenname=DumbHippoBot&message=Hello+Bot">
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
		All done? <a href="home">Return to your page.</a>
		</p>
		
	</div>
	
	
</body>
</html>
