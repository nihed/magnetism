<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<table cellspacing="0" cellpadding="0" class="dh-tip">
	<tr>
	<td width="13" class="dh-tip-icon" valign="top">
		<dh:png src="/images3/${buildStamp}/star.png" style="width: 13; height: 13"/>
	</td>
	<td>
		<div class="dh-tip-tip">
			<jsp:doBody/>
		</div>
	</td>
</table>