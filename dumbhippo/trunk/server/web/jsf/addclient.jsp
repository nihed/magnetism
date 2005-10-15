<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title>Sign in</title>
	</head>
	<body>
    <h:form>
	    <h3>Your email address</h3>
		<tr>
			<td>Name:</td>
			<td><h:inputText value="#{addclient.email}" /></td>
		</tr>
		<!-- without an input field we lose this when we post -->
		<h:inputHidden value="#{addclient.goBackTo}"/>
		<p><h:commandButton value="Sign in" action="#{addclient.doAddClient}" /></p>
	</h:form>
	</body>
</f:view>
</html>
