<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:choose>
	<c:when test="${browser.windows}">
		<div><dh:png src="/images3/${buildStamp}/windows_tooltray.png" style="width: 182px; height: 48px; padding: 5px 0px;"/></div>
	</c:when>
	<c:when test="${browser.linux}">
		<div><dh:png src="/images3/${buildStamp}/linux_tooltray.png" style="width: 156px; height: 44px; padding: 5px 0px;"/></div>
	</c:when>
</c:choose>
Mugshot icon in tool tray	
