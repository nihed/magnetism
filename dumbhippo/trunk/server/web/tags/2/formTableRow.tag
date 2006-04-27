<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="label" required="true" type="java.lang.String" %>

<tr valign="top">
	<td class="dh-label-cell"><c:out value="${label}"/>:</td>
	<td class="dh-control-cell"><div class="dh-control-cell-div"><jsp:doBody/></div></td></tr>
</tr>
