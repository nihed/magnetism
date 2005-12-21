<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<div id="dhToolbar">
<c:url value="/home" var="home"/>
<c:url value="/account" var="account"/>
Do It: <a class="dh-toolbar-item" href="${home}">Home</a> &#151; <a class="dh-toolbar-item" href="${account}">Your Account</a>
<jsp:doBody/>
</div>
