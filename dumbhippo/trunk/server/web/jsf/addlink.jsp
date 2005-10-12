<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
	<head>
	<title>Add Link</title>
	</head>
	<body>
	<h:form>
		<h3>Please enter the URL to share, a comment, and audience:</h3>
		<table>
			<tr>
				<td>URL:</td>
				<td><h:inputText value="#{addlink.URL}" /></td>
			</tr>
			<tr>
				<td>Comment:</td>
				<td><h:inputText value="#{addlink.comment}" /></td>
			</tr>
			<tr>
				<td>Recipients:</td>
				<td><h:inputText value="#{addlink.recipients}" /></td>
			</tr>
		</table>
		<p><h:commandButton value="Add Link" action="#{addlink.doAddLink}" />
		</p>
	</h:form>
	</body>
</f:view>
</html>
