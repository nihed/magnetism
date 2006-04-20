<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="boxClass" required="false" type="java.lang.String" %>
<%@ attribute name="more" required="false" type="java.lang.String" %>

<div class="dh-sidebar-box ${boxClass}">
	<div class="dh-title"><c:out value="${title}"/></div>
	<jsp:doBody/>
	<c:if test="${!empty more}">
		<div class="dh-more"><a href="${more}">MORE</a></div>
	</c:if>
</div>
