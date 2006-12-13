<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
 
<td align="left" valign="top" width="75%">
<table cellspacing="0" cellpadding="0">
<tr>
<td valign="top">
<dh:png klass="dh-stacker-block-icon" src="${block.icon}" style="width: 16; height: 16; border: none;"/>
</td>
<td valign="top">
<div class="dh-stacker-block-left">	
<jsp:doBody/>
</div>
<td>
</tr>
</table>
</td>
<td width="0%">&nbsp;</td>
