<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="label" required="true" type="java.lang.String" %>

<tr valign="top">
	<td class="dh-label-cell"><c:if test="${!empty label}"><c:out value="${label}"/>:</c:if></td>
	<td class="dh-control-cell"><div class="dh-control-cell-div"><jsp:doBody/></div></td></tr>
</tr>
<%-- there's no good multibrowser way to do this with CSS [think row margins will work - OWT ] --%>
<tr><td colspan="2" class="dh-spacer-row"></td></tr>
