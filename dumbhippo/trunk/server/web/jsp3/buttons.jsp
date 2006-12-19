<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:set var="pageName" value="Buttons" scope="page"/>

<c:choose>
	<%-- this is set in request scope since some of the tags used only on this page use it --%>
	<c:when test="${signin.valid}">
		<c:set var="buttonLink" value="${baseUrl}/person?who=${signin.user.id}" scope="request"/>
	</c:when>
	<c:otherwise>
		<c:set var="buttonLink" value="${baseUrl}/" scope="request"/>
	</c:otherwise>
</c:choose>

<head>
	<title><c:out value="${pageName} - Mugshot"/></title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="buttons"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="buttons">
	<dht3:shinyBox color="grey">
		<div class="dh-buttons-header-box">
			<div class="dh-buttons-header-image">
			</div>
			<div class="dh-buttons-header-text">
				<div>
					Can't get enough Mugshot? Tell the world and build your network. (And ours!)
				</div>
				<div>
					Add our badges to your site or blog, linking to your Mugshot page. Instructions are below.
				</div>
			</div>
		</div>

		<div class="dh-buttons-main-box">
			<div class="dh-buttons-choices-box">
				<dht3:buttonPreviewAndCode image="mugshot_50x50.gif"      description="50x50 AIM icon" imageWidth="50" imageHeight="50"/>
				<dht3:buttonPreviewAndCode image="mugshot_80x15.gif"      description="80x15" 		   imageWidth="80" imageHeight="15"/>
				<dht3:buttonPreviewAndCode image="mugshot_88x31.gif"      description="88x31"          imageWidth="88" imageHeight="31"/>
				<dht3:buttonPreviewAndCode image="mugshotlove_88x31.gif"  description="88x31"          imageWidth="88" imageHeight="31"/>
				<dht3:buttonPreviewAndCode image="mugshot_94x20.gif"      description="94x20"          imageWidth="94" imageHeight="20"/>
				<dht3:buttonPreviewAndCode image="mugshot_120x60.gif"     description="120x60"         imageWidth="120" imageHeight="60"/>
				<dht3:buttonPreviewAndCode image="mugshotlove_120x60.gif" description="120x60"         imageWidth="120" imageHeight="60"/>
				<dht3:buttonPreviewAndCode image="mugshot_120x90.gif"     description="120x90"         imageWidth="120" imageHeight="90"/>				
				<dht3:buttonPreviewAndCode image="mugshotlove_120x90.gif" description="120x90"         imageWidth="120" imageHeight="90"/>
				<dht3:buttonPreviewAndCode image="mugshot_120x240.gif"    description="120x240"        imageWidth="120" imageHeight="240"/>				
			</div>
			
			<div class="dh-buttons-instructions-box">
				<div class="dh-buttons-instructions-headline">How to add buttons in...</div>
				<dht3:buttonInstructions title="Blogger">
					<ol>
						<li>Do it</li>
						<li>Do it</li>
					</ol>
				</dht3:buttonInstructions>
				<dht3:buttonInstructions title="MySpace">
					<ol>
						<li>Do it</li>
						<li>Do it</li>
					</ol>
				</dht3:buttonInstructions>				
			</div>
		
			<div class="dh-grow-div-around-floats"></div>
		</div>

		<div class="dh-grow-div-around-floats"></div>
	</dht3:shinyBox>
</dht3:page>

</html>
