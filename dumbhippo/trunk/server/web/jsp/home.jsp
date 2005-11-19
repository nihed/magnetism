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
		<dht:uploadPhoto location="/headshots" linkText="Change My Photo"/>
	</div>

	<div id="dhMain">
		<table>
		<tr>
		<td>
			<c:if test="${home.receivedPosts.size > 0}">
				<div class="shared-links">	
					<strong>Links Shared With You</strong>
					<dht:postList posts="${home.receivedPosts.list}" maxPosts="${home.maxReceivedPostsShown}" recipientId="${home.person.user.id}" recipientName="${home.person.name}"/>
				</div>
			</c:if>
		</td>
		<td>
			<c:if test="${home.contactPosts.size > 0}">
				<div class="shared-links">
					<strong>Links Shared By Your Friends</strong>
					<c:forEach items="${home.contactPosts.list}" var="post">
						<dht:postBubble post="${post}"/>
					</c:forEach>
				</div>
			</c:if>
		</td>
		</tr>
		</tr>
		<td>
			<c:if test="${home.groups.size > 0}">
				<div class="groups">
					<strong>Groups You're In</strong>
					<br/>
					<dh:entityList value="${home.groups.list}"/>
				</div>
			</c:if>
			<c:if test="${home.contacts.size > 0}">
				<div class="friends">
					<strong>People You Know</strong>
					<br/>
					<dh:entityList value="${home.contacts.list}"/>
				</div>
			</c:if>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
