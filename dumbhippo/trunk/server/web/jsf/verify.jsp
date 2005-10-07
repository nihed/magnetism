<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>

<f:view>
	<head>
	<title>Verify</title>
	</head>
	<body>
	<h:form>
		<h3>verify page</h3>
		<p>This is the verify page.</p>
		<p>This should parse out the authKey from the request parameters and
		mark the user verified or not in the database as appropriate, then
		tell them how it went.</p>
		<p>If successful, should print a list of users who invited them.</p>
		<p><h:commandButton value="Go back to the main page" action="mainpage" /></p>
	</h:form>
	</body>
</f:view>
</html>
