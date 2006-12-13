<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="icon" required="false" type="java.lang.String" %>
<%@ attribute name="info" required="false" type="java.lang.String" %>
<%@ attribute name="altRow" required="false" type="java.lang.Boolean" %>

<c:set var="rowClass" value="dh-base-row"/>

<c:if test="${altRow}">
    <c:set var="rowClass" value="dh-alt-row"/>
</c:if>

<tr><td colspan="3" class="dh-padding-row ${rowClass}"></td></tr>

<tr valign="top" class="${rowClass}">
	<td class="dh-label-cell">
		<c:if test="${!empty icon}"><dh:png klass="dh-form-table-row-icon" src="${icon}" style="width: 16; height: 16; border: none;"/></c:if>
	    <c:if test="${!empty label}"><c:out value="${label}"/>:</c:if>
	</td>
	<c:choose>
	    <c:when test="${!empty info}">
	        <td class="dh-control-cell dh-control-cell-next-to-info"><div class="dh-control-cell-div"><jsp:doBody/></div></td>
	        <td class="dh-info-cell"><c:out value="${info}"/></td>
	    </c:when>
	    <c:otherwise>
	        <td colspan="2" class="dh-control-cell"><div class="dh-control-cell-div"><jsp:doBody/></div></td>
	    </c:otherwise>
	</c:choose>         
</tr>

<tr><td colspan="3" class="dh-padding-row ${rowClass}"></td></tr>

<%-- there's no good multibrowser way to do this with CSS [think row margins will work - OWT ] --%>
<tr><td colspan="3" class="dh-spacer-row"></td></tr>
