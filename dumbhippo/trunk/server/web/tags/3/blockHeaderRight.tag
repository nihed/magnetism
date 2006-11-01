<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="from" required="false" type="com.dumbhippo.server.views.EntityView" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<td align="right" valign="middle" id="dhStackerBlockRightContainer-${blockId}">
<div>
	<div class="dh-stacker-block-close" id="dhStackerBlockClose-${blockId}">
		<a href="javascript:dh.stacker.blockClose('${blockId}')">CLOSE</a> <a href="javascript:dh.stacker.blockClose('${blockId}')"><img src="/images3/${buildStamp}/close.png"/></a>
	</div>
	<table cellspacing="0" cellpadding="0">
	<tr>
	<td><div class="dh-stacker-block-right">
		<c:if test="${showFrom}">
			<div class="dh-stacker-block-right-from-name"><dht3:entityLink who="${from}" onlineIcon="true"/></div>
		</c:if>
		<jsp:doBody/>
	</div></td>
	<td valign="top">
		<c:if test="${showFrom}">
			<span class="dh-stacker-block-right-from-image"><dht3:entityLink who="${from}" imageOnly="true"/></span>
		</c:if>
	</td>
	</tr>
	</table>
</div>