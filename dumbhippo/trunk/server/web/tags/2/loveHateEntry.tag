<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="userInfoType" required="true" type="java.lang.String" %>
<%@ attribute name="isInfoTypeProvidedBySite" required="true" type="java.lang.Boolean" %>
<%@ attribute name="link" required="true" type="java.lang.String" %>
<%@ attribute name="baseId" required="true" type="java.lang.String" %>
<%@ attribute name="accountId" required="true" type="java.lang.String" %>
<%@ attribute name="mode" required="true" type="java.lang.String" %>
<%@ attribute name="mugshotEnabled" required="true" type="java.lang.Boolean" %>

<%-- Note: <jsp:doBody/> is used only when an account is loved to dispay additional details --%>
<%-- because right now it is only useful when an account is loved. It's ok to use it when an --%>
<%-- account is not loved if necessary. The only difference would be that we will not remove --%>
<%-- Amazon wish list and review details when someone is editing an account and selects "hate it", --%>
<%-- but doesn't yet apply the change. --%>

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
	    <c:if test="${!mugshotEnabled}">
	        &nbsp;
            <a href="javascript:dh.love.setMode('${baseId}', 'loveEdit')" title="Click to change">
		      (not used)
		    </a>   
		</c:if>  
	    <jsp:doBody/>
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
		<c:if test="${mode == 'love'}">
		    <c:set var="title" value="You can list multiple accounts of a given type, but only up to one of them can be used on Mugshot at the moment."/>
		    <div class="dh-account-preferences-row dh-mugshot-enabled-preference">
		        <c:choose>
		            <c:when test="${mugshotEnabled}">
                        <input type="checkbox" id="${baseId}MugshotEnabled" checked
			                   onclick="dh.account.toggleMugshotEnabled('${accountId}');" title="${title}">		            
		            </c:when>
		            <c:otherwise>
                        <input type="checkbox" id="${baseId}MugshotEnabled"
			                   onclick="dh.account.toggleMugshotEnabled('${accountId}');" title="${title}">			            
		            </c:otherwise>
		        </c:choose>    
			    <label for="${baseId}MugshotEnabled" title="${title}">Use on Mugshot</label>
			</div>
		</c:if>	
	    <jsp:doBody/>
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
