<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Tour</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="tour"/>	
</head>

<dht3:page currentPageLink="tour">
   	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Mugshot Tour</div>
	    <div id="dhTourMovie">
			<object classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000" codebase="http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=8,0,0,0" width="600" height="400" id="tour" align="middle">
				<param name="allowScriptAccess" value="sameDomain" />
				<param name="movie" value="/flash/tour.swf" />
				<param name="quality" value="high" />
				<param name="bgcolor" value="#ffffff" />
				<embed src="/flash/tour.swf" quality="high" bgcolor="#ffffff" width="600" height="400" name="tour" align="middle" allowScriptAccess="sameDomain" type="application/x-shockwave-flash" pluginspage="http://www.macromedia.com/go/getflashplayer" />
			</object>
		</div>
	</dht3:shinyBox>
</dht3:page>
