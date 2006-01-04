<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="home" required="false" type="java.lang.Boolean" %>
<%@ attribute name="account" required="false" type="java.lang.Boolean" %>
<%@ attribute name="publicPageId" required="false" type="java.lang.String" %>

<% /* seems like there should be a better way to do defaults */ %>
<c:if test="${empty home}">
	<c:set var="home" value="true"/>
</c:if>
<c:if test="${empty account}">
	<c:set var="account" value="true"/>
</c:if>

<div id="dhToolbar">
Do It:
<c:if test="${home}">
	<a class="dh-toolbar-item" href="/home">Home</a>
</c:if>
<c:if test="${home && account}">
&#151;
</c:if>
<c:if test="${account}">
	<a class="dh-toolbar-item" href="/account">Your Account</a>
</c:if>
<c:if test="${account && !empty publicPageId || home && !empty publicPageId}">
&#151;
</c:if>
<c:if test="${!empty publicPageId}">
	<a class="dh-toolbar-item" href="/person?who=${publicPageId}">Your Public Page</a>
</c:if>
<jsp:doBody/>
</div>
