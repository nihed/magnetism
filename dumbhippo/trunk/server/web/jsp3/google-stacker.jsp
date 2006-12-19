<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:set var="pageName" value="Google Gadget" scope="page"/>

<head>
	<title>${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="google-stacker">
	<dht3:shinyBox color="grey">
		<%-- FIXME show google stacker sample with the "Add to Google" button.
			This is easily done once the gadget works by going to Google itself
			and clicking "get the code" to get the syndicated gadget code. --%>
		<a href="/google-stacker-spec">xml spec</a>, <a href="/google-stacker-content">iframe content</a>
	</dht3:shinyBox>
</dht3:page>

</html>
