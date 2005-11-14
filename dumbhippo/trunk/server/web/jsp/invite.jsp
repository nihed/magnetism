<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Invite a Friend</title>
	<dht:stylesheets />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header>
		Inviting a Friend
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<c:url value="sendinvite" var="submiturl"/>
		<form action="${submiturl}" method="post">
			<h3>Invite someone by email:</h3>
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
	</div>
</body>
</html>
