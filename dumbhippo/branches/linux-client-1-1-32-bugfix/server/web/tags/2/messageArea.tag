<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@ attribute name="idSuffix" required="false" type="java.lang.String" %>

<c:if test="${empty idSuffix}">
	<c:set var="idSuffix" value=""/>
</c:if>

<div class="dh-message" id="dhMessageDiv${idSuffix}" style='display: ${(!empty param["message"] && (idSuffix == "")) ? "block" : "none"};'>
    <c:out value='${param["message"]}'/>
</div>