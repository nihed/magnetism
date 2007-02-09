<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>

<span class="dh-stacker-block-time">
	<span id="dhStackerBlockTimeAgoQualifier-${blockId}" class="dh-stacker-block-time-qualifier">active </span>${block.timeAgo}
</span>
