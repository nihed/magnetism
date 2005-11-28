<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="toolbar">
	<c:url value="/home" var="home"/>
	<c:url value="/account" var="account"/>
	Do It: <a href="${home}">&#187; Home</a> &#151; <a href="${account}">Your Account</a>
	<jsp:doBody/>
</div>
