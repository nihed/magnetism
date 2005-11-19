<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="home" class="com.dumbhippo.web.HomePage" scope="request"/>

<head>
	<title><c:out value="${home.person.name}"/></title>
	<dht:stylesheets href="/css/home.css" iehref="/css/home-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
	        dojo.require("dh.util");
	</script>
</head>
<body>
    <c:url value="viewperson?personId=${home.signin.user.id}" var="publicurl"/>
	<dht:header>
		This is You!<br/>
		<a style="font-size:8pt;" href="${publicurl}">(your public page)</a>
	</dht:header>
	<dht:toolbar/>
	<div class="person">
		<dht:png klass="cool-person" src="/files/headshots/${home.person.viewPersonPageId}" />
		<dht:userNameEdit value="${home.person.name}"/>
	</div>
	<div>
		<dht:uploadPhoto location="/headshots"/>
		<a id="dhChangeMyPhotoLink" href="javascript:void(0);" onClick="dh.util.swapElements('dhPhotoUploadFileEntry','dhChangeMyPhotoLink')">Change my photo</a>
	</div>

	<div id="dhMain">
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Links Shared With You</strong>
				<dht:postList posts="${home.receivedPosts}" maxPosts="${home.maxReceivedPostsShown}" recipientId="${home.person.user.id}" recipientName="${home.person.name}"/>
			</div>
		</td>
		<td>
			<div class="shared-links">	
				<strong>Links Shared By Your Friends</strong>
				<c:forEach items="${home.contactPosts}" var="post">
					<dht:postBubble post="${post}"/>
				</c:forEach>
			</div>
		</td>
		</tr>
		</tr>
		<td>
			<div class="groups">
				<strong>Groups You're In</strong>
				<br/>
				<dh:entityList value="${home.groups}"/>
			</div>
			<div class="friends">
				<strong>People You Know</strong>
				<br/>
				<dh:entityList value="${home.contacts}"/>
			</div>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
