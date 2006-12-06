<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- id of the control this is the status for --%>
<%@ attribute name="controlId" required="true" type="java.lang.String" %>

<tr id="${controlId}StatusRow" valign="top" style="display: none;">
	<td colspan="3"><div class="dh-notification dh-save-status"><span id="${controlId}StatusText"><jsp:doBody/></span> <a id="${controlId}StatusLink" href=""></a></div></td>
</tr>
<%-- there's no good multibrowser way to do this with CSS --%>
<tr id="${controlId}StatusSpacer" style="display: none;"><td colspan="3" class="dh-spacer-row"></td></tr>
