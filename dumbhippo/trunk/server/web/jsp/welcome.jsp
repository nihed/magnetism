<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.WelcomePage" scope="request"/>

<head>
	<title><c:out value="${welcome.personInfo.humanReadableName}"/></title>
	<link rel="stylesheet" href="/css/home.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
    <c:url value="viewperson?personId=${welcome.signin.user.id}" var="publicurl"/>
    <dht:header>
		This is You!<br/>
		<a style="font-size:8pt"href="${publicurl}">(your public page)</a>
	</dht:header>

	<dht:toolbar/>

	<p>Welcome to DumbHippo</p>
	
	<p>
		DumbHippo is a new way to share things with your friends. You can
		see what people have shared with you on this web page, but to get the
		full experience, you'll want to download and install our software.
		Once you've installed the DumbHippo software, you'll get notification 
		when someone shares a link or message with you, and you'll be able
		to share the web pages you are browsing with other people.
	</p>
	<input type="button" 
	       value="Install the dumbhippo.com software" 
	       onclick='window.open("${welcome.downloadUrlWindows}", "_self")'>       
	<p>
		Other people currently see you as: 
		<dht:userNameEdit value="${welcome.personInfo.humanReadableName}"/>.
		(<small>Click name to edit</small>)
	</p>
	<div class="main">
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Cool Shared Links</strong>
				<c:forEach items="${welcome.receivedPostInfos}" var="info">
					<dht:postBubble info="${info}"/>
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
