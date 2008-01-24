<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<dh:default var="showFrom" value="true"/>

<td align="left" valign="top" width="${showFrom ? (chatHeader ? 70 : (oneLine ? 80 : 75)) : 100}%">
<table cellspacing="0" cellpadding="0" width="100%">
<tr>
<td valign="top" class="dh-stacker-block-icon-column"><dh:png klass="dh-stacker-block-icon" src="${block.icon}" style="width: 16; height: 16; border: none;"/></td>
<td valign="top">
<div class="dh-stacker-block-left">	
<jsp:doBody/>
</div>
</td>
</tr>
</table>
</td>
<td width="0%">&nbsp;</td>
