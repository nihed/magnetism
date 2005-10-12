<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title>Invite</title>
	</head>
	<body>
	<h:form>
		<h3>Please enter the name and email of the user to invite:</h3>
		<table>
			<tr>
				<td>Name:</td>
				<td><h:inputText value="#{invite.fullName}" /></td>
			</tr>
			<tr>
				<td>Email:</td>
				<td><h:inputText value="#{invite.email}" /></td>
			</tr>
		</table>
		<p><h:commandButton value="Invite" action="#{invite.doInvite}" /></p>
	</h:form>
	</body>
</f:view>
</html>
