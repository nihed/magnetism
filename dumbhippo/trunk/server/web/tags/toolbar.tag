<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="toolbar">
	<c:url value="/home" var="home"/>
	Do It: <a href="${home}">&#187; Home</a>
	<jsp:doBody/>
</div>
