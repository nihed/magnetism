<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="id" required="true" type="java.lang.String" rtexprvalue="false"%>
<%@ attribute name="label" required="true" type="java.lang.String"%>
<%@ attribute name="value" required="false" type="java.lang.String"%>
<%@ attribute name="contents" required="false" fragment="true"%>

<tr>
	<td class="dh-application-edit-label">
		<label for="${id}"><c:out value="${label}"/>: </label>
	</td>
	<td class="dh-application-edit-control">
		<c:choose>
			<c:when test="${!empty contents}">
				<jsp:invoke fragment="contents"/>
			</c:when>
			<c:otherwise>
				<c:out value="${value}"/>
			</c:otherwise>
		</c:choose>
	</td>
</tr>
