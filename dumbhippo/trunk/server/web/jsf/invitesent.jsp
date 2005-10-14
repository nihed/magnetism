<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title>Invite Sent</title>
	</head>
	<body>
	<h:form>
		<h3>Congratulations, the invitation to 
		    <h:outputText value="#{invite.fullName}"/>
		    (<h:outputText value="#{invite.email}"/>) was sent.</h3>
		<!--  print the link now -->
		<c:url value="verify.faces?authKey=${invite.authKey}" var="authurl"/>
		<p>Invite url: <a href="${authurl}">${authurl}</a>
        </p>
		<p><h:commandLink action="main">
			<h:outputText value="Go back to the main page" />
		</h:commandLink></p>
	</h:form>
	</body>
</f:view>
</html>
