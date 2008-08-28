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
			<span id="${baseId}LoveValueId"></span>
		</a>
	</div>
	<div id="${baseId}IndifferentId" style="display: ${indifferentDisplay};">
		<a href="javascript:dh.love.setMode('${baseId}', 'loveEdit')" title="Add an account"><dh:png klass="dh-add-icon" src="/images3/${buildStamp}/add_icon.png" style="width: 10; height: 10; overflow: hidden;" />
		Add an account</a>
	</div>
	<div id="${baseId}LoveEditId" style="display: none;">
		<dht:textInput id="${baseId}LoveEntryId" maxlength="255"/>
		<img src="/images-gnome/${buildStamp}/save_button.gif" onclick="dh.love.saveClicked('${baseId}', 'love')"/>
		<a href="javascript:dh.love.cancelClicked('${baseId}')" title="Remove the account"><img src="/images-gnome/${buildStamp}/x_button.gif"/></a>
        <div dhId="DescriptionNormal">
            Enter your  
	        <jsp:element name="a">
		        <jsp:attribute name="href"><c:out value="${link}"/></jsp:attribute>
			    <jsp:attribute name="target">_blank</jsp:attribute>
		 	    <jsp:body><c:out value="${name}"/></jsp:body>
		    </jsp:element>
		    <c:out value="${userInfoType}"/>
		</div>    		    
	</div>	
	<div id="${baseId}BusyId" style="display: none;">
		<img src="/images-gnome/${buildStamp}/feedspinner.gif"/> Please wait...
	</div>	
</div>