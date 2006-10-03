<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.PostBlockView" %>

<div class="dh-stacker-block-content-post">
	<table cellspacing="0" cellpadding="0" width="100%">
	<tr>
	<td>&nbsp;</td>
	<td align="right" class="dh-stacker-block-content-controls">
	<c:choose>
		<c:when test="${block.postView.livePost.totalViewerCount == 1}">1 view</c:when>
		<c:otherwise>${block.postView.livePost.totalViewerCount} views</c:otherwise>
	</c:choose>
	<c:if test="${signin.valid}">
		<c:choose>
			<c:when test="${block.postView.favorite}">
				<c:if test="${favesMode != 'add-only'}">
				 | <a href="javascript:dh.actions.setPostFavorite('${block.postView.post.id}', false);">Remove as Fave</a>
				</c:if>
			</c:when>
			<c:otherwise>
				| <a href="javascript:dh.actions.setPostFavorite('${block.postView.post.id}', true);">Add to Faves</a>
			</c:otherwise>
		</c:choose>
	  | <jsp:element name="a">
	  	  <jsp:attribute name="href">javascript:dh.util.openShareLinkWindow(<dh:jsString value="${block.postView.post.url}"/>, <dh:jsString value="${block.postView.post.title}"/>);</jsp:attribute>
	  	  <jsp:body>Web Swarm this</jsp:body>
	  	</jsp:element>
	</c:if>	
	</div>
	</td>
	</tr>
	</table>
	<div class="dh-stacker-block-content-main">	
	<span class="dh-stacker-block-content-post-chatting"><c:out value="${block.postView.livePost.chattingUserCount}"/></span> people chatting <dht:actionLinkChat prefix=" | " oneLine="true" chatId="${block.postView.post.id}" kind="post"/>
	</div>
</div>
