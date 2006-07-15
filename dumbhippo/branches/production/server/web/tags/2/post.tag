<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView" %>
<%@ attribute name="favesMode" required="false" type="java.lang.String" %>
<%@ attribute name="includeExtra" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showPhoto" required="false" type="java.lang.Boolean" %>
<%@ attribute name="numeral" required="false" type="java.lang.Integer" %>

<c:if test="${showPhoto}">
	<c:set var="itemClass" value="dh-item-with-photo" scope="page"/>
</c:if>
<c:if test="${!empty numeral}">
	<c:set var="itemClass" value="dh-item-with-numeral" scope="page"/>
</c:if>

<div class="dh-item ${itemClass}">
	<c:if test="${showPhoto}">
		<div class="dh-image">
			<dht:headshot person="${post.poster}"/>
		</div>
	</c:if>
	<c:if test="${!empty numeral}">
		<div class="dh-numeral">
			<c:out value="${numeral}"/>
		</div>
	</c:if>
	<div class="dh-next-to-image">
		<div class="dh-title">
			<a href="${post.url}" onClick="return dh.util.openFrameSet(window,event,this,'${post.post.id}');"
				title="${post.url}">
				<c:out value="${post.titleAsHtml}" escapeXml="false"/>
			</a>
		</div>
		<div class="dh-blurb">
			<c:out value="${post.textAsHtml}" escapeXml="false"/>
		</div>
		<c:if test="${empty includeExtra || includeExtra}">
			<div class="dh-extra-info">
				<table cellpadding="0" cellspacing="0">
					<tbody>
						<tr>
							<td align="left">
							    <%-- FIXME: tagify this and share with framer --%>
								<div class="dh-attribution">
									sent by <a href="${post.poster.homeUrl}" class="dh-name-link">
										<c:out value="${post.poster.name}"/>
									</a>
							        to 
							        <c:choose>
										<c:when test="${post.toWorld}">
											<c:set var="recipientsPrefix" value="The World" scope="page"/>
										</c:when>
										<c:otherwise>
											<c:set var="recipientsPrefix" value="" scope="page"/>					
										</c:otherwise>
									</c:choose>
							        <dh:entityList prefixValue="${recipientsPrefix}" value="${post.recipients}" separator=", "/>			
								</div>
							</td>
							<td align="right">
								<%-- FIXME --%>
								<div class="dh-counts">
									<c:choose>
										<c:when test="${post.livePost.totalViewerCount == 1}">
											1 view
										</c:when>
										<c:otherwise>
											${post.livePost.totalViewerCount} views
										</c:otherwise>
									</c:choose>
									<c:choose>
										<c:when test="${favesMode == 'none' || !signin.valid}">
											
										</c:when>
										<c:when test="${post.favorite}">
											<c:if test="${favesMode != 'add-only'}">
											 | <a href="javascript:dh.actions.setPostFavorite('${post.post.id}', false);">remove</a>
											</c:if>
										</c:when>
										<c:otherwise>
											| <a href="javascript:dh.actions.setPostFavorite('${post.post.id}', true);">add to faves</a>
										</c:otherwise>
									</c:choose>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</c:if>
	</div>
	<div class="dh-grow-div-around-floats"><div></div></div>
</div>
