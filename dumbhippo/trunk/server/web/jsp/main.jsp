<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="signin" class="com.dumbhippo.web.SigninBean" scope="session"/>

<head>
	<title>Main</title>
	<link rel="stylesheet" href="/css/group.css" type="text/css" />
	<script src="/javascript/config.js" type="text/javascript"></script>
    <script src="/javascript/dojo/dojo.js" type="text/javascript"></script>
    <script src="/javascript/common.js" type="text/javascript"></script>
    <script type="text/javascript">
	    dojo.require("dh.server");	    
	    function dhSignOut() {
	    	dh.server.doPOST("signout", { },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't sign out");
				  	    	 });
	    }
    </script>	
</head>
<body>
	<h3>This is the main page.</h3>

    <c:choose>
 	<c:when test="${signin.valid}">
	 	<c:url value="home" var="homeurl"/>
	   	<p><a href="${homeurl}">Your home page</a></p>
	   	<p><a href="javascript:dhSignOut()">Sign out</a>/</p>
  	</c:when>
	<c:otherwise>
	 	<c:url value="signin?next=home" var="signinurl"/>
  		<p><a href="${signinurl}">Sign in to DumbHippo</a></p>
 	</c:otherwise>
	</c:choose>
</body>
</html>
