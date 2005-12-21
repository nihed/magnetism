<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="signin" class="com.dumbhippo.web.SigninBean" scope="request"/>

<head>
	<title>Main</title>
	<dht:stylesheets />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header>
		Main
	</dht:header>
	<dht:toolbar/>

	<h3>This is the main page.</h3>

    <c:choose>
		<c:when test="${signin.valid}">
			<c:url value="home" var="homeurl"/>
		   	<p><a href="${homeurl}">Your home page</a></p>
			<p><a href="javascript:dh.actions.signOut()">Sign out</a></p>
	  	</c:when>
		<c:otherwise>
	 		<c:url value="who-are-you?next=home" var="signinurl"/>
	  		<p><a href="${signinurl}">Sign in to DumbHippo</a></p>
		</c:otherwise>
	</c:choose>
</body>
</html>
