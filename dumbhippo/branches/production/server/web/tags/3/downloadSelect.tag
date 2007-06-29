<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="id" required="true" type="java.lang.String" %>
<%@ attribute name="onchange" required="true" type="java.lang.String" %>
<%@ attribute name="style" required="false" type="java.lang.String" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${disabled}">
		<select id="${id}" onchange="${onchange}" style="${style}" disabled="disabled"><jsp:doBody/></select>
	</c:when>
	<c:otherwise>
		<select id="${id}" onchange="${onchange}" style="${style}"><jsp:doBody/></select>
	</c:otherwise>
</c:choose>
