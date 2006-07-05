<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="baseId" required="true" type="java.lang.String" %>
<%@ attribute name="mode" required="true" type="java.lang.String" %>

<%--  the Javascript manages this visibility also, but we want to get it right on page load --%>
<c:choose>
	<c:when test="${mode == 'love'}">
		<c:set var="loveDisplay" value="block" scope="page"/>
		<c:set var="hateDisplay" value="none" scope="page"/>
		<c:set var="indifferentDisplay" value="none" scope="page"/>
	</c:when>
	<c:when test="${mode == 'hate'}">
		<c:set var="loveDisplay" value="none" scope="page"/>
		<c:set var="hateDisplay" value="block" scope="page"/>
		<c:set var="indifferentDisplay" value="none" scope="page"/>
	</c:when>
	<c:when test="${mode == 'indifferent'}">
		<c:set var="loveDisplay" value="none" scope="page"/>
		<c:set var="hateDisplay" value="none" scope="page"/>
		<c:set var="indifferentDisplay" value="block" scope="page"/>
	</c:when>	
	<c:otherwise>
		<dht:errorPage>Internal error (mode = ${mode}), sorry!</dht:errorPage>
	</c:otherwise>
</c:choose>

<div id="${baseId}AllId">
	<div id="${baseId}LoveId" style="display: ${loveDisplay};">
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'loveEdit')" title="Click to change">
			<dh:png src="/images2/${buildStamp}/loveicon16x14.png" style="width: 16px; height: 14px;"/>
			<span id="${baseId}LoveValueId"></span>
		</a>
	</div>
	<div id="${baseId}HateId" style="display: ${hateDisplay};">
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'hateEdit')" title="Click to change">
			<dh:png src="/images2/${buildStamp}/hateicon16x14.png" style="width: 16px; height: 14px;"/>
			<span id="${baseId}HateValueId"></span>
		</a>
	</div>	
	<div id="${baseId}IndifferentId" style="display: ${indifferentDisplay};">
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'loveEdit')" title="Express your love"><dh:png src="/images2/${buildStamp}/loveicon16x14.png" style="width: 16px; height: 14px;"/>
		I love it!</a>
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'hateEdit')" title="Swear undying hatred"><dh:png src="/images2/${buildStamp}/hateicon16x14.png" style="width: 16px; height: 14px;"/>
		I hate it!</a>		
	</div>
	<div id="${baseId}LoveEditId" style="display: none;">
		<dh:png src="/images2/${buildStamp}/loveicon16x14.png" style="width: 16px; height: 14px;"/>
		<dht:textInput id="${baseId}LoveEntryId"/>
		<input type="button" value="Save" onclick="dh.lovehate.saveClicked('${baseId}', 'love')"/>
		<a href="javascript:dh.lovehate.cancelClicked('${baseId}')" title="I don't love it anymore - go back to being indifferent"><img src="/images2/${buildStamp}/x.gif"/></a>
	</div>	
	<div id="${baseId}HateEditId" style="display: none;">
		<dh:png src="/images2/${buildStamp}/hateicon16x14.png" style="width: 16px; height: 14px;"/>
		<dht:textInput id="${baseId}HateEntryId" maxlength="255"/>
		<input type="button" value="Save" onclick="dh.lovehate.saveClicked('${baseId}', 'hate')"/>
		<a href="javascript:dh.lovehate.cancelClicked('${baseId}')" title="End the hate - go back to being indifferent"><img src="/images2/${buildStamp}/x.gif"/></a>
	</div>
	<div id="${baseId}BusyId" style="display: none;">
		<img src="/images2/${buildStamp}/feedspinner.gif"/> Please wait...
	</div>	
</div>
