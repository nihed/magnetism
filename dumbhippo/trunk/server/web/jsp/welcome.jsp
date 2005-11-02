<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.WelcomePage" scope="request"/>

<head>
	<title>Welcome <c:out value="${welcome.person.humanReadableName}"/>!</title>
	<link rel="stylesheet" href="/css/welcome.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
    <c:url value="viewperson?personId=${welcome.signin.user.id}" var="publicurl"/>
    <dht:header>
		Welcome!
	</dht:header>

	<dht:toolbar/>

	<div class="dh-welcome-message">
		<p class="dh-welcome-headline">Welcome to DumbHippo</p>

			<div class="dh-big-download-box">
				<div class="dh-center-children">
					<a href="${welcome.downloadUrlWindows}">Download Now</a>
				</div>
				DumbHippo software is free.
			</div>
	
		<p>
	
			DumbHippo is a new way to share things with your friends. You can
			see what people have shared with you on this web page, but to get the
			full experience, you'll want to download and install our software.
			Once you've installed the DumbHippo software, you'll get notification 
			when someone shares a link or message with you, and you'll be able
			to share the web pages you are browsing with other people.
		</p>
		<p>
			Click <a href="${welcome.downloadUrlWindows}">Download Now</a> to try it out!
		</p>
	</div>
	
	<div class="main">
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Cool Shared Links</strong>
				<c:forEach items="${welcome.receivedPosts}" var="post">
					<dht:postBubble post="${post}"/>
				</c:forEach>
			</div>
		</td>
		<td>
			<div class="groups">
				<strong>Groups You're In</strong><br/>
				<dh:entityList value="${welcome.groups}"/>
			</div>
			<div class="friends">
				<strong>People who invited you</strong><br/>
				<dh:entityList value="${welcome.inviters}"/>
			</div>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
