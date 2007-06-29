<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="sendlabel" required="true" type="java.lang.String" %>
<%@ attribute name="onsend" required="true" type="java.lang.String" %>
<%@ attribute name="multiline" required="false" type="java.lang.Boolean" %>

<div id="dhChatSentiments">
	<span id="dhChatIndifferent" class="dh-chat-sentiment dh-chat-sentiment-selected" onselectstart="return false;">
	    <dh:png src="/images3/${buildStamp}/comment_iconchat_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
	    Quip
    </span>
	<span id="dhChatLove" class="dh-chat-sentiment" onselectstart="return false;">
	    <dh:png src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/>
		I love it!
	</span>
	<span id="dhChatHate" class="dh-chat-sentiment" onselectstart="return false;">
	    <dh:png src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
		I hate it!
	</span>
</div>
<table id="dhChatInputTable" cellspacing="0" cellpadding="0">
<tr>
<td id="dhChatMessageInputCell">
	<c:choose>
		<c:when test="${multiline}">
			<textarea id="dhChatMessageInput"></textarea>
		</c:when>
		<c:otherwise>
			<input type="text" id="dhChatMessageInput"></input>
		</c:otherwise>
	</c:choose>
</td>
<td>
	<input id="dhChatAddButton" type="button" value="${sendlabel}" onclick="${onsend}"/>
</td>
</tr>
</table>

