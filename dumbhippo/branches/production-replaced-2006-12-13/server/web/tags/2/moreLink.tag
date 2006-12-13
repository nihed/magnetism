<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="more" required="true" type="java.lang.String" %>
<%@ attribute name="moreName" required="false" type="java.lang.String" %>

<c:if test="${empty moreName}">
	<c:set var="moreName" value="MORE" scope="page"/>
</c:if>

<div class="dh-more"><a href="${more}"><c:out value="${moreName}"/></a></div>
