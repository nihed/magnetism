<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%-- from attr isn't really required if showFrom=false, but right now everything provides from anyway, and 
     I'm not sure how to throw if showFrom=true and from is empty --%>
<%@ attribute name="from" required="true" type="com.dumbhippo.server.views.EntityView" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>

<c:set var="imageSize" value="${chatHeader ? 60 : 30}"/>

<td align="right" valign="top" id="dhStackerBlockRightContainer-${blockId}" class="dh-stacker-block-right-container" width="25%">
<div class="dh-stacker-block-right-container-inner">
	<table cellspacing="0" cellpadding="0" class="dh-stacker-block-right-container-inner-table">
	<tr>
	<td valign="top"><div class="dh-stacker-block-right">
		<c:if test="${showFrom}">
			<div class="dh-stacker-block-right-from-name">from <dht3:entityLink who="${from}" onlineIcon="true"/></div>
		</c:if>
		<jsp:doBody/>
	</div></td>
	<c:if test="${showFrom}">
	    <td valign="top" align="right" width="${imageSize}px">
			<span class="dh-stacker-block-right-from-image"><dht3:entityLink who="${from}" imageOnly="true" imageSize="${imageSize}"/></span>
	    </td>
	    <td width="5px">&nbsp;</td>
	</c:if>
	</tr>
	</table>
</div>
</td>
