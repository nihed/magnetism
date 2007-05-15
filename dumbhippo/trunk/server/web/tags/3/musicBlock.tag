<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.MusicBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dh:default var="chatHeader" value="false"/>

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
								<dht3:trackLink track="${track}"><c:out value="${track.artist}"/></dht3:trackLink>
								<span> - </span>
								<dht3:trackLink track="${track}"><c:out value="${track.name}"/></dht3:trackLink>
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
		<c:choose>
			<c:when test="${!empty track.artist && !empty track.name}">
				<c:set var="title" value="${track.artist} - ${track.name}"/>
			</c:when>
			<c:when test="${!empty track.artist}">
				<c:set var="title" value="${track.artist}"/>
			</c:when>
			<c:when test="${!!empty track.name}">
				<c:set var="title" value="${track.name}"/>
			</c:when>
			<c:otherwise>
				<c:set var="title" value="Unknown Song"/>
			</c:otherwise>
		</c:choose>
		<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${!chatHeader}" title="${title}">
			<td class="dh-stacker-block-with-image-left" align="left" valign="top" width="75%">
				<table cellspacing="0" cellpadding="0" width="100%">
				<tr>
				<td valign="top" class="dh-block-image-cell" width="${imageWidth}">
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
					<div class="dh-stacker-block-with-image-beside">	
						<div>
							<div class="dh-music-block-artist"><dht3:trackLink track="${track}"><c:out value="${track.artist}"/></dht3:trackLink></div>
							<div class="dh-music-block-name"><dht3:trackLink track="${track}"><c:out value="${track.name}"/></dht3:trackLink></div>
							<c:if test="${!chatHeader}">
								<dht3:quipper blockId="${blockId}" block="${block}"/>
								<dht3:stackReason block="${block}" blockId="${blockId}"/>
							</c:if>
						</div>
					</div>
				</td>
				</tr>
				</table>
				<dht3:blockContent blockId="${blockId}">
					<c:if test="${!chatHeader}">
						<dht3:chatPreview block="${block}" blockId="${blockId}"/>
					</c:if>
					<div class="dh-music-block-history">
						<c:forEach items="${block.oldTracks}" var="historyTrack">
							<div class="dh-music-block-history-item">
								<span class="dh-music-block-history-artist"><dht3:trackLink track="${historyTrack}"><c:out value="${historyTrack.artist}"/></dht3:trackLink></span>
								<span class="dh-music-block-history-separator"> - </span>
								<span class="dh-music-block-history-name"><dht3:trackLink track="${historyTrack}"><c:out value="${historyTrack.name}"/></dht3:trackLink></span>
								&nbsp;<span class="dh-stacker-block-time-ago"><c:out value="${historyTrack.lastListenString}"/></span>
							</div>
						</c:forEach>
					</div>
				</dht3:blockContent>			
			</td>
			<td width="0%">&nbsp;</td>
			<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}" chatHeader="${chatHeader}">
				<c:if test="${!chatHeader}">
					<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
				</c:if>
				<dh:forEachDownload track="${track}" var="download" varStatus="status">
					<div>
						<c:if test="${status.first}">
							Play with:
						</c:if>
						<a class="dh-music-source-link" href="${dh:xmlEscape(download.url)}"><c:out value="${download.source.shortName}"/></a>
					</div>
				</dh:forEachDownload>				
				<dht3:blockControls blockId="${blockId}">
					&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
				</dht3:blockControls>				
			</dht3:blockRight>
		</dht3:blockContainer>
	</c:otherwise>
</c:choose>
	
