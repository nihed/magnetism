<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="identifySelf" required="false" type="java.lang.Boolean"%>

<c:if test="${empty identifySelf}">
	<c:set var="identifySelf" value="false"/>
</c:if>	

<c:if test="${!empty who.viewPersonPageId}">
	<c:set var="personLink" value="/person?who=${who.viewPersonPageId}" scope="page"/>
</c:if>	


<c:choose>
    <c:when test="${identifySelf && who.viewOfSelf}">
        <c:set var="personName" value="you"/>
    </c:when>
    <c:otherwise>
        <c:set var="personName" value="${who.name}"/>
    </c:otherwise>
</c:choose>  
            
<span class="dh-name">
    <c:choose>
        <c:when test="${!empty personLink}">
            <a href="${personLink}">
            <c:out value="${personName}"/>        
            </a>
        </c:when>
        <c:otherwise>
            <c:out value="${personName}"/>
        </c:otherwise>
    </c:choose>
</span>

