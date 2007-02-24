<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="page" required="true" type="java.lang.String" %>

<c:if test="${empty param['who']}">
	<c:choose>
		<c:when test="${signin.valid}">
			<c:redirect url="${page}">
				<c:param name="who" value="${signin.userId}"/>
				<%-- this seems a little implausible, but appears to work --%>
				<c:forEach var="pair" items="${param}">
					<c:param name="${pair.key}" value="${pair.value}"/>
				</c:forEach>
			</c:redirect>
		</c:when>
		<c:otherwise>
			<dht:errorPage>Whose page would you like? (URL should have who=USERID parameter)</dht:errorPage>
		</c:otherwise>
	</c:choose>
</c:if>
