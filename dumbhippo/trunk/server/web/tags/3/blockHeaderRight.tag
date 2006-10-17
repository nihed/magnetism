<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="icon" required="false" type="java.lang.String" %>

<td align="right">
<div>
	<div class="dh-stacker-block-close" id="dhStackerBlockClose-${blockId}">
		<a href="javascript:dh.stacker.blockClose('${blockId}')">CLOSE</a> <a href="javascript:dh.stacker.blockClose('${blockId}')"><img src="/images3/${buildStamp}/close.png"/></a>
	</div>
	<table cellspacing="0" cellpadding="0">
	<tr>
	<td><div class="dh-stacker-block-right"><jsp:doBody/></div></td>
	<td><c:if test="${!empty icon}">
			<img src="${icon}"/>
		</c:if></td>
	</tr>
	</table>
</div>