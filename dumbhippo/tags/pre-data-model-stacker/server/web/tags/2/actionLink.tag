<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="id" required="false" type="java.lang.String" %>
<%@ attribute name="href" required="true" type="java.lang.String" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>
<%@ attribute name="underline" required="false" type="java.lang.Boolean" %>

<c:set var="tagName" value="div"/>
	
<c:if test="${oneLine}">
	<c:set var="tagName" value="span"/>
</c:if>
	
<c:if test="${! empty id}">
	<c:set var="idAttribute" value="id=\"${id}\""/>
</c:if>

<c:if test="${disabled}">
	<c:set var="disabledClass" value="dh-action-link-disabled"/>
</c:if>

<c:if test="${underline}">
    <c:set var="underlineClass" value="dh-underlined-link"/>
</c:if>

<dh:script module="dh.html"/>
<${tagName} class="dh-action-link">
    <a ${idAttribute} class="${disabledClass} ${underlineClass}" href="${href}" onclick="return !dh.html.hasClass(this, 'dh-action-link-disabled');" title="${title}"><jsp:doBody/></a>
</${tagName}>
