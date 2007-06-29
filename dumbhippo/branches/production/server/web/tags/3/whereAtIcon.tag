<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="linkText" required="false" type="java.lang.String" %>
<%@ attribute name="linkTarget" required="true" type="java.lang.String" %>
<%@ attribute name="imgSrc" required="true" type="java.lang.String" %>

<c:set var="imgTitle" value="${label}"/>
<c:if test="${!empty linkText}">
	<c:set var="imgTitle" value="${label}: ${linkText}"/>
</c:if>

<a href="${linkTarget}">
    <dh:png src="${imgSrc}" title="${imgTitle}" style="width: 16; height: 16; border: none;"/>
</a>