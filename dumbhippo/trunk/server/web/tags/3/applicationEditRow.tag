<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="id" required="true" type="java.lang.String" rtexprvalue="false"%>
<%@ attribute name="label" required="true" type="java.lang.String"%>
<%@ attribute name="name" required="true" type="java.lang.String" rtexprvalue="false"%>
<%@ attribute name="value" required="false" type="java.lang.String"%>
<%@ attribute name="onchange" required="false" type="java.lang.String"%>
<%@ attribute name="onkeyup" required="false" type="java.lang.String"%>
<%@ attribute name="rowClass" required="false" type="java.lang.String"%>
<%@ attribute name="help" required="false" fragment="true"%>
<%@ attribute name="contents" required="false" fragment="true"%>
<%@ attribute name="multiline" required="false" type="java.lang.Boolean"%>

<tr class="${rowClass}">
	<td class="dh-application-edit-label">
		<label for="${id}"><c:out value="${label}"/>: </label>
	</td>
	<td class="dh-application-edit-control">
		<c:choose>
			<c:when test="${!empty contents}">
				<jsp:invoke fragment="contents"/>
			</c:when>
			<c:when test="${multiline}">
			    <textarea id="${id}" name="${name}" onchange="${onchange}"><c:out value="${value}"/></textarea>
			</c:when>
			<c:otherwise>
			    <jsp:element name="input">
				    <jsp:attribute name="id">${id}</jsp:attribute>
				    <jsp:attribute name="name">${name}</jsp:attribute>
				    <jsp:attribute name="value"><c:out value="${value}"/></jsp:attribute>
				    <jsp:attribute name="onchange">${onchange}</jsp:attribute>
				    <jsp:attribute name="onkeyup">${onkeyup}</jsp:attribute>
			    </jsp:element>
			</c:otherwise>
		</c:choose>
	</td>
</tr>
<c:if test="${!empty help}">
	<tr class="${rowClass}">
		<td></td>
		<td class="dh-application-edit-help">
			<jsp:invoke fragment="help"/>
		</td>
	</tr>
</c:if>
