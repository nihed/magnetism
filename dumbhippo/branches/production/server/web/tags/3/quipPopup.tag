<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${signin.valid}">
	<dh:script module="dh.quippopup"/>
	<script type="text/javascript">
		dh.quippopup.selfName = "${signin.viewedUser.name}";
		dh.quippopup.selfHomeUrl = "${signin.viewedUser.homeUrl}";
		function dhQuipPopupInit() {
			dh.quippopup.init();
		}
		dh.event.addPageLoadListener(dhQuipPopupInit);
	</script>
	
	<div id="dhQuipPopup" style="display: none;">
		<div id="dhQuipPopupTitle"></div>
	    <div id="dhQuipPopupClose" onclick="dh.quippopup.cancel()">
	    	<dh:png src="/images3/${buildStamp}/closetip.png" style="width: 9px; height: 9px"/>
	   	</div>
		<dht3:chatInput multiline="false" sendlabel="Quip!" onsend="dh.quippopup.send()"/>
	</div>
</c:if>

