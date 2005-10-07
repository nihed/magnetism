<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>

<f:view>
	<head>
	<title>Invite Sent</title>
	</head>
	<body>
	<h:form>
		<h3>Congratulations, the invitation to "${invite.fullName}" (${invite.email}) was sent.</h3>
		<!--  print the link now -->
		<p>Invite url:
			<a href="/dumbhippo/jsf/verify.faces?auth=${invite.authKey}">/dumbhippo/jsf/verify.faces?auth=${invite.authKey}</a>
        </p>
		<p><h:commandLink action="mainpage">
			<h:outputText value="Go back to the main page" />
		</h:commandLink></p>
	</h:form>
	</body>
</f:view>
</html>
