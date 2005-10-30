<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<head>
	<title>Invite</title>
</head>
<body>
	<c:url value="sendinvite" var="submiturl"/>
	<form action="${submiturl}" method="post">
		<h3>Please enter the name and email of the user to invite:</h3>
		<table>
			<tr>
				<td>Name:</td>
				<td><input name="fullName"></td>
			</tr>
			<tr>
				<td>Email:</td>
				<td><input name="email"></td>
			</tr>
		</table>
		<input type="submit" value="Invite"/>
	</form>
</body>
</html>
