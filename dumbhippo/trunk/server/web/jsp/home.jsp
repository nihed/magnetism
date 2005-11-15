<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="home" class="com.dumbhippo.web.HomePage" scope="request"/>

<head>
	<title><c:out value="${home.person.humanReadableName}"/></title>
	<dht:stylesheets href="/css/home.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
	        dojo.require("dh.util");
	</script>
</head>
<body>
    <c:url value="viewperson?personId=${home.signin.user.id}" var="publicurl"/>
	<dht:header>
		This is You!<br/>
		<a style="font-size:8pt"href="${publicurl}">(your public page)</a>
	</dht:header>
	<dht:toolbar/>
	<div class="person">
		<dht:png klass="cool-person" src="/files/headshots/${home.person.person.id}" />
		<dht:userNameEdit value="${home.person.humanReadableName}"/>
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
				<c:forEach items="${home.receivedPosts}" var="post" varStatus="status">
					<dht:postBubble post="${post}"/>
					  <c:if test="${status.last}">
			    <div style="text-align:right"><input style="width:7em;border:1px solid black;" type="text" value="Search"/> the <a href="/shares">other shares</a> sent to you.</div>
					  </c:if>  
				</c:forEach>
			</div>
		</td>
		<td>
			<div class="shared-links">	
				<strong>Links Shared By your friends</strong>
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
