<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>

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
		<p>Invite url:
		    <h:outputLink value="/dumbhippo/jsf/verify.faces?authKey=#{invite.authKey}">
		      <h:outputText value="/dumbhippo/jsf/verify.faces?authKey=#{invite.authKey}"/>
		    </h:outputLink>
        </p>
		<p><h:commandLink action="mainpage">
			<h:outputText value="Go back to the main page" />
		</h:commandLink></p>
	</h:form>
	</body>
</f:view>
</html>
