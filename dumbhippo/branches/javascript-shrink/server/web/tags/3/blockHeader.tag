<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="icon" required="true" type="java.lang.String" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<table class="dh-stacker-block-header" cellspacing="0" cellpadding="0">
	<tr><td align="left" width="20px"><dh:png klass="dh-stacker-block-icon" src="${icon}" style="width: 16; height: 16; border: none;"/></td>
	<jsp:doBody/>
	</tr>
</table>	
