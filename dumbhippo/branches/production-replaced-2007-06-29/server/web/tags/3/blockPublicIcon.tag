<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>

<c:if test="${!block.public}">
	<span class="dh-stacker-block-title-lock">
		<dh:png src="/images3/${buildStamp}/lock_icon.png" style="width: 12px; height: 14px; border: none;" title="${block.privacyTip}"/>
	</span>
</c:if>	