<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<tr valign="top">
	<td colspan="4"><jsp:doBody/></td>
</tr>

<%-- there's no good multibrowser way to do this with CSS [think row margins will work - OWT ] --%>
<tr><td colspan="4" class="dh-spacer-row"></td></tr>
