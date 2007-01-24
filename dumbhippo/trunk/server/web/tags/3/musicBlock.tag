<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.MusicBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showChat" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dh:default var="showChat" value="true"/>

<c:set var="track" value="${block.track}"/>
<c:set var="imageWidth" value="${track.smallImageUrlAvailable ? track.smallImageWidth : 60}"/>

<c:choose>
	<c:when test="${oneLine}">
		<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}">
		       <dht3:blockLeft block="${block}">
	               <dht3:blockTitle>
		       	       <c:choose>
		       			   <c:when test="${block.quip}">
								Quip:
								<a href="${block.personSource.homeUrl}">
									<c:out value="${track.artist}"/> - <c:out value="${track.name}"/>
								</a>
							</c:when>
							<c:otherwise>
								<a href="${track.artistPageLink}"><c:out value="${track.artist}"/></a>
								<span> - </span>
								<a href="${track.artistPageLink}"><c:out value="${track.name}"/></a>
							</c:otherwise>
						</c:choose>
	               </dht3:blockTitle>
		       </dht3:blockLeft>
		       <dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
		               <dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
		       </dht3:blockRight>
		</dht3:blockContainer>
	</c:when>
	<c:otherwise>
		<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${showChat || !block.quip}">
			<td class="dh-music-block-left" align="left" valign="top" width="75%">
				<table cellspacing="0" cellpadding="0" width="100%">
				<tr>
				<td valign="top" class="dh-music-block-icon" width="${imageWidth}">
					<c:choose>
						<c:when test="${track.smallImageUrlAvailable}">
							<img src="${track.smallImageUrl}" width="${imageWidth}" height="${track.smallImageHeight}"/>
						</c:when>
						<c:otherwise>
							<img src="/images3/${buildstamp}/noart.png" width="60" height="60"/>
						</c:otherwise>
					</c:choose>
				</td>
				<td valign="top">
					<div class="dh-music-block-beside">	
						<div>
							<div class="dh-music-block-artist"><a href="${track.artistPageLink}"><c:out value="${track.artist}"/></a></div>
							<div class="dh-music-block-name"><a href="${track.artistPageLink}"><c:out value="${track.name}"/></a></div>
							<c:if test="${showChat && !empty block.lastMessage}">
								<dht3:chatMessage id="dhMusicBlockMessage-${blockId}" msg="${block.lastMessage}"/>
							</c:if>
						</div>
					</div>
				</td>
				</tr>
				</table>
				<dht3:blockContent blockId="${blockId}">
					<c:if test="${showChat}">
						<dht3:chatPreview block="${block}" chatId="${track.playId}" chatKind="music" chattingCount="0" showChatLink="${false}"/>
					</c:if>
					<c:forEach items="${block.oldTracks}" var="track">
						<div class="dh-music-block-history-item">
							<span class="dh-music-block-history-artist"><a href="${track.artistPageLink}"><c:out value="${track.artist}"/></a></span>
							<span class="dh-music-block-history-separator"> - </span>
							<span class="dh-music-block-history-name"><a href="${track.artistPageLink}"><c:out value="${track.name}"/></a></span>
							&nbsp;<span class="dh-stacker-block-time-ago"><c:out value="${track.lastListenString}"/></span>
						</div>
					</c:forEach>
				</dht3:blockContent>			
			</td>
			<td width="0%">&nbsp;</td>
			<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
				<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
				<dht3:blockControls blockId="${blockId}">
					&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
				</dht3:blockControls>				
			</dht3:blockRight>
		</dht3:blockContainer>
	</c:otherwise>
</c:choose>
	
