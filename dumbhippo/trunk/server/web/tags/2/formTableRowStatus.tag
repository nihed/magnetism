<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- id of the control this is the status for --%>
<%@ attribute name="controlId" required="true" type="java.lang.String" %>
<%@ attribute name="statusLinkCount" required="false" type="java.lang.Integer" %>

<c:if test="${empty statusLinkCount}">
    <c:set var="statusLinkCount" value="1"/>
</c:if>

<tr id="${controlId}StatusRow" valign="top" style="display: none;">
	<td colspan="4"><div class="dh-notification dh-save-status"><span id="${controlId}StatusText"><jsp:doBody/></span> 
    <%-- the end value is inclusive --%>
	<c:forEach varStatus="loopStatus" begin="1" end="${statusLinkCount}" step="1">
	    <a id="${controlId}StatusLink${loopStatus.count}" href=""></a>
	</c:forEach>
	</div></td>
</tr>
<%-- there's no good multibrowser way to do this with CSS --%>
<tr id="${controlId}StatusSpacer" style="display: none;"><td colspan="4" class="dh-spacer-row"></td></tr>
