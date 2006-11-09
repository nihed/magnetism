<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="from" required="false" type="com.dumbhippo.server.views.EntityView" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<td align="right" valign="top" id="dhStackerBlockRightContainer-${blockId}" class="dh-stacker-block-right-container" width="25%">
<div class="dh-stacker-block-right-container-inner">
	<table cellspacing="0" cellpadding="0" class="dh-stacker-block-right-container-inner-table">
	<tr>
	<td><div class="dh-stacker-block-right">
		<c:if test="${showFrom}">
			<div class="dh-stacker-block-right-from-name"><dht3:entityLink who="${from}" onlineIcon="true"/></div>
		</c:if>
		<jsp:doBody/>
	</div></td>
	<td valign="top" align="right" width="30px">
		<c:if test="${showFrom}">
			<span class="dh-stacker-block-right-from-image"><dht3:entityLink who="${from}" imageOnly="true"/></span>
		</c:if>
	</td>
	<td width="5px">&nbsp;</td>
	</tr>
	</table>
</div>
</td>