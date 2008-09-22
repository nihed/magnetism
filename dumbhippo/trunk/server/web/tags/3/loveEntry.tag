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
		<c:set var="indifferentDisplay" value="none" scope="page"/>
	</c:when>
	<c:when test="${mode == 'hate' || mode == 'indifferent'}">
		<c:set var="loveDisplay" value="none" scope="page"/>
		<c:set var="indifferentDisplay" value="block" scope="page"/>
	</c:when>	
	<c:otherwise>
		<dht:errorPage>Internal error (mode = ${mode}), sorry!</dht:errorPage>
	</c:otherwise>
</c:choose>

<div id="${baseId}AllId" class="dh-love-hate">
	<div id="${baseId}LoveId" style="display: ${loveDisplay};">
		<a href="javascript:dh.love.setMode('${baseId}', 'loveEdit')" title="Click to change">
		    <dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/> 
			<span id="${baseId}LoveValueId"></span>
		</a>
	</div>
	<div id="${baseId}IndifferentId" style="display: ${indifferentDisplay};">
		<a href="javascript:dh.love.setMode('${baseId}', 'loveEdit')" title="Add another account"><dh:png klass="dh-add-icon" src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>Add another account</a>
	</div>
	<div id="${baseId}LoveEditId" style="display: none;">
	    <dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>
		<dht:textInput id="${baseId}LoveEntryId" maxlength="255"/>
		<img src="/images3/${buildStamp}/save_button.gif" onclick="dh.love.saveClicked('${baseId}', 'love')"/>
		<a href="javascript:dh.love.cancelClicked('${baseId}')" title="Remove the account"><img src="/images3/${buildStamp}/x_button.gif"/></a>
        <div dhId="DescriptionNormal">
            <div class="dh-love-hate-instruction-editing">
				Editing <c:out value="${name}"/>.<br/>	
			</div>
			<a style="font-weight: bold;" href="javascript:dh.lovehate.setMode('${baseId}', 'loveEdit')" title="Click to change">
				<dh:png src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/> Love it			
           	</a>: Enter your 
			<c:choose>
				<c:when test="${isInfoTypeProvidedBySite}">  
				    <jsp:element name="a">
					    <jsp:attribute name="href"><c:out value="${link}"/></jsp:attribute>
					    <jsp:attribute name="target">_blank</jsp:attribute>
					    <jsp:body><c:out value="${name}"/></jsp:body>
				    </jsp:element> <c:out value="${userInfoType}"/><span dhId="AccountHelpId"></span>.
				</c:when>
				<c:otherwise>
				    <c:out value="${userInfoType}"/>
				    <jsp:element name="a">
					    <jsp:attribute name="href"><c:out value="${link}"/></jsp:attribute>
					    <jsp:attribute name="target">_blank</jsp:attribute>
					    <jsp:body><c:out value="${name}"/></jsp:body>
				    </jsp:element> 
				    account<span dhId="AccountHelpId"></span>.				
				</c:otherwise>
		    </c:choose>    
			<%-- We use dhId because this tag is used in multiple places. Not sure if this is currently used. --%>
			<span dhId="LoveTipId"></span>
		</div>    		    
	</div>	
	<div id="${baseId}BusyId" style="display: none;">
		<img src="/images2/${buildStamp}/feedspinner.gif"/> Please wait...
	</div>	
</div>