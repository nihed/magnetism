<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="href" required="true" type="java.lang.String" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>

<c:set var="tagName" value="div"/>
	
<c:if test="${oneLine}">
	<c:set var="tagName" value="span"/>
</c:if>

<${tagName} class="dh-action-link">
<a href="${href}" title="${title}"><jsp:doBody/></a>
</${tagName}>
