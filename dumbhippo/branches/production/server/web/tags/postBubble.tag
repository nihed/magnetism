<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView"%>
<%@ attribute name="hidePoster" required="false" type="java.lang.Boolean"%>
<%@ attribute name="hideRecipientId" required="false" type="java.lang.String"%>
<%@ attribute name="noBorder" required="false" type="java.lang.Boolean"%>

<c:if test="${!noBorder}">
	<div class="dh-share-shadow">
	<div class="dh-share">
</c:if>
	<div class="dh-share-from">
		<dh:entity value="${post.poster}" photo="true" bodyLengthLimit="14"/>
	</div>
	<div class="dh-share-text">
		<a href="${post.url}" onClick="return dh.util.openFrameSet(window,event,this,'${post.post.id}');" title="${post.url}" class="dh-share-link"><c:out value="${post.titleAsHtml}" escapeXml="false"/></a>
		<div class="dh-share-description">
			<c:out value="${post.textAsHtml}" escapeXml="false"/>
			<c:if test="${!empty post.livePost}">
				<c:set var="totalAtPost" value="${post.livePost.chattingUserCount + post.livePost.viewingUserCount}" scope="page"/>
				<c:if test="${totalAtPost > 1}">
					&nbsp; <span style="font-size: smaller; font-weight: bold;">${totalAtPost} people checking this out</span>
				</c:if>
			</c:if>
		</div>
	</div>
	<div class="dh-share-to"><dh:favicon link="${post.url}"/>  
		<dh:skipList value="${post.recipients}" skipId="${hideRecipientId}">
			Sent to <dh:entityList value="${post.recipients}" separator=", "/>
		</dh:skipList>
	</div>
<c:if test="${!noBorder}">	
	</div>
	</div>
</c:if>	
