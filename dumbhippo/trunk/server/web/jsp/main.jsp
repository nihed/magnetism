<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>

<dh:bean id="signin" class="com.dumbhippo.web.SigninBean" scope="request"/>

<head>
	<title>Main</title>
	<link rel="stylesheet" href="/css/group.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
	<h3>This is the main page.</h3>

    <c:choose>
		<c:when test="${signin.valid}">
			<c:url value="home" var="homeurl"/>
		   	<p><a href="${homeurl}">Your home page</a></p>
			<p><a href="javascript:dh.actions.signOut()">Sign out</a></p>
	  	</c:when>
		<c:otherwise>
	 		<c:url value="signin?next=home" var="signinurl"/>
	  		<p><a href="${signinurl}">Sign in to DumbHippo</a></p>
		</c:otherwise>
	</c:choose>
</body>
</html>
