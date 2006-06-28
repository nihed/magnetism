<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="multiline" required="false" type="java.lang.Boolean" %>
<%@ attribute name="extraClass" required="false" type="java.lang.String" %>
<%@ attribute name="type" required="false" type="java.lang.String" %>
<%@ attribute name="id" required="false" type="java.lang.String" %>
<%@ attribute name="name" required="false" type="java.lang.String" %>
<%@ attribute name="disabled" required="false" type="java.lang.String" %>
<%@ attribute name="maxlength" required="false" type="java.lang.Integer" %>
<%@ attribute name="style" required="false" type="java.lang.String" %>

<c:if test="${empty type}">
	<c:set var="type" value="text" scope="page"/>
</c:if>

<c:if test="${disabled}">
	<c:set var="disabledAttribute" value="disabled"/>
</c:if>

<c:if test="${empty dhTextInputCount}">
	<c:set var="dhTextInputCount" value="0" scope="request"/>
</c:if>
<c:set var="dhTextInputCount" value="${dhTextInputCount + 1}" scope="request"/>
<c:set var="N" value="${dhTextInputCount}" scope="page"/>

<c:if test="${empty id}">
	<c:set var="id" value="dhTextInput${N}" scope="page"/>
</c:if>
<c:if test="${empty name}">
	<c:set var="name" value="${id}" scope="page"/>
</c:if>

<c:if test="${empty maxlength}">
	<c:set var="maxlength" value="64" scope="page"/>
</c:if>

<c:choose>
	<c:when test="${multiline}">
		<textarea ${disabledAttribute} id="${id}" name="${name}" class="dh-text-input ${extraClass}" rows="5" style="${style}"></textarea>
	</c:when>
	<c:otherwise>
		<input ${disabledAttribute} id="${id}" name="${name}" type="${type}" class="dh-text-input ${extraClass}" maxlength="${maxlength}" style="${style}"/>
		
	</c:otherwise>
</c:choose>
