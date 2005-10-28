<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<head>
	<title>Invite Sent</title>
</head>
<body>
		<h3>Congratulations, the invitation to 
		    <c:out value="${fullName}"/>
		    (<c:out value="${email}"/>) was sent.</h3>
		<!--  print the link now -->
		<c:url value="verify?authKey=${authKey}" var="authurl"/>
		<p>Invite url: <a href="${authurl}">${authurl}</a>
        </p>
        
        <c:url value="main" var="mainurl"/>
		<p><a href="${mainurl}">Go back to the main page</a></p>
</body>
</html>
