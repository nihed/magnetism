<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="userInfoType" required="true" type="java.lang.String" %>
<%@ attribute name="isInfoTypeProvidedBySite" required="true" type="java.lang.Boolean" %>
<%@ attribute name="link" required="true" type="java.lang.String" %>
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

<div id="${baseId}AllId" class="dh-love-hate">
	<div id="${baseId}LoveId" style="display: ${loveDisplay};">
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'loveEdit')" title="Click to change">
			<dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>
			<span id="${baseId}LoveValueId"></span>
		</a>
	</div>
	<div id="${baseId}HateId" style="display: ${hateDisplay};">
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'hateEdit')" title="Click to change">
			<dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
			<span id="${baseId}HateValueId"></span>
		</a>
	</div>
	<div id="${baseId}IndifferentId" style="display: ${indifferentDisplay};">
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'loveEdit')" title="Express your love"><dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>
		I love it!</a>
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'hateEdit')" title="Swear undying hatred"><dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
		I hate it!</a>
		<a href="javascript:dh.lovehate.setMode('${baseId}', 'indifferentInfo')" title="Tell me more"><dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/info_icon.png" style="width: 11; height: 11; overflow: hidden;"/></a>
	</div>
	<div id="${baseId}IndifferentInfoId" style="display: none;">
		<dht:loveHateEntryDescription isEditing="false" baseId="${baseId}" name="${name}" userInfoType="${userInfoType}" isInfoTypeProvidedBySite="${isInfoTypeProvidedBySite}" link="${link}"/>		
	</div>
	<div id="${baseId}LoveEditId" style="display: none;">
		<dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>
		<dht:textInput id="${baseId}LoveEntryId" maxlength="255"/>
		<img src="/images3/${buildStamp}/save_button.gif" onclick="dh.lovehate.saveClicked('${baseId}', 'love')"/>
		<a href="javascript:dh.lovehate.cancelClicked('${baseId}')" title="I don't love it anymore - go back to being indifferent"><img src="/images3/${buildStamp}/x_button.gif"/></a>
		<dht:loveHateEntryDescription isEditing="true" baseId="${baseId}" name="${name}" userInfoType="${userInfoType}" isInfoTypeProvidedBySite="${isInfoTypeProvidedBySite}" link="${link}"/>
	</div>	
	<div id="${baseId}HateEditId" style="display: none;">
		<dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
		<dht:textInput id="${baseId}HateEntryId" maxlength="255"/>
		<img src="/images3/${buildStamp}/save_button.gif" onclick="dh.lovehate.saveClicked('${baseId}', 'hate')"/>
		<a href="javascript:dh.lovehate.cancelClicked('${baseId}')" title="End the hate - go back to being indifferent"><img src="/images3/${buildStamp}/x_button.gif"/></a>
		<dht:loveHateEntryDescription isEditing="true" baseId="${baseId}" name="${name}" userInfoType="${userInfoType}" isInfoTypeProvidedBySite="${isInfoTypeProvidedBySite}" link="${link}"/>		
	</div>
	<div id="${baseId}BusyId" style="display: none;">
		<img src="/images2/${buildStamp}/feedspinner.gif"/> Please wait...
	</div>	
</div>
