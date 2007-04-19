<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<c:if test="${!empty block.chatId && signin.valid}">
	<div class="dh-quipper">
		<a class="dh-quip-indifferent dh-quip-sentiment"  href="javascript:void(0)" onclick="dh.stacker.quip(event, '${blockId}', '${block.chatId}', dh.control.SENTIMENT_INDIFFERENT)">
		    <dh:png src="/images3/${buildStamp}/comment_iconchat_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
		    Quip
	    </a>
		<a class="dh-quip-love dh-quip-sentiment"  href="javascript:void(0)" onclick="dh.stacker.quip(event, '${blockId}', '${block.chatId}', dh.control.SENTIMENT_LOVE)">
		    <dh:png src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>
			I love it!
		</a>
		<a class="dh-quip-hate dh-quip-sentiment"  href="javascript:void(0)" onclick="dh.stacker.quip(event, '${blockId}', '${block.chatId}', dh.control.SENTIMENT_HATE)">
		    <dh:png src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
			I hate it!
		</a>
	</div>
</c:if>