<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="toolbar">
	<c:url value="/sharelink?next=home" var="share"/>
	DoIt: <a href="${share}">&#187; Share</a> &#151; <a href="/family">Your Family Page</a>
	<jsp:doBody/>
</div>
