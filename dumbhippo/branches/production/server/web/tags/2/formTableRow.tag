<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="icon" required="false" type="java.lang.String" %>
<%@ attribute name="prefixIcon" required="false" type="java.lang.String" %>
<%@ attribute name="prefixIconWidth" required="false" type="java.lang.Integer" %>
<%@ attribute name="prefixIconHeight" required="false" type="java.lang.Integer" %>
<%@ attribute name="info" required="false" type="java.lang.String" %>
<%@ attribute name="altRow" required="false" type="java.lang.Boolean" %>
<%@ attribute name="controlId" required="false" type="java.lang.String" %>

<c:set var="rowClass" value="dh-base-row"/>

<c:if test="${altRow}">
    <c:set var="rowClass" value="dh-alt-row"/>
</c:if>

<c:if test="${! empty prefixIcon}">
    <c:set var="withPrefixClass" value="dh-label-cell-div-with-prefix"/>
</c:if>

<tr><td colspan="4" class="dh-padding-row ${rowClass}"></td></tr>

<tr valign="top" class="${rowClass}" id="${controlId}FormContainer"> 
	<td class="dh-label-cell">
		<div class="dh-label-cell-div ${withPrefixClass}" id="${controlId}FormLabel">
            <c:if test="${!empty prefixIcon}"><dh:png klass="dh-form-table-row-icon" src="${prefixIcon}" style="width: ${prefixIconWidth}; height: ${prefixIconHeight}; border: none; overflow: hidden;"/></c:if>
			<c:if test="${!empty icon}"><dh:png klass="dh-form-table-row-icon" src="${icon}" style="width: 16; height: 16; border: none; overflow: hidden;"/></c:if>
		    <c:if test="${!empty label}"><c:out value="${label}"/>:</c:if>
	    </div>
	</td>
	<c:choose>
	    <c:when test="${!empty info}">
	        <td class="dh-control-cell dh-control-cell-next-to-info"><div class="dh-control-cell-div"><jsp:doBody/></div></td>
	        <td class="dh-info-cell"><c:out value="${info}"/></td>
	    </c:when>
	    <c:otherwise>
	        <td colspan="2" class="dh-control-cell">
				<div class="dh-control-cell-div" id="${controlId}FormContent"><jsp:doBody/></div>
		    </td>
	    </c:otherwise>
	</c:choose>
	<td class="dh-box-spacer">&nbsp;</td>      
</tr>

<tr><td colspan="4" class="dh-padding-row ${rowClass}"></td></tr>

<%-- there's no good multibrowser way to do this with CSS [think row margins will work - OWT ] --%>
<tr><td colspan="4" class="dh-spacer-row"></td></tr>
