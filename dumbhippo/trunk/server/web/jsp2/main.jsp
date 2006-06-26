<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/main.css"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
	<script type="text/javascript">
		var onMouseIn = function(node, rolloverSrc) {
			if (rolloverSrc != node.src) {
				node.savedSrc = node.src;
				node.src = rolloverSrc;
			}
		}
		var onMouseOut = function(node, oldSrc) {
			if (oldSrc != node.src) {
				node.src = oldSrc;
			}
		}
		
		var setupRollover = function(nodeId, rolloverSrc) {
			var node = document.getElementById(nodeId);
			var oldUrl = node.src;
			node.onmouseover = function() {
				onMouseIn(node, rolloverSrc);
			}
			node.onmouseout = function() {
				onMouseOut(node, oldUrl);
			}
		}
		
		var init = function() {
			setupRollover('dhHeaderWeb', '/images2/${buildStamp}/header_linkhome2.gif');
			setupRollover('dhHeaderMusic', '/images2/${buildStamp}/header_musichome2.gif');
			setupRollover('dhHeaderTv', '/images2/${buildStamp}/header_tvhome2.gif');
		}
	</script>
</head>
<%-- can use dht:body once we don't need onload here --%>
<body onload="init();" class="dh-gray-background-page dh-main-page">
	<div id="dhPage">

		<dht:header kind="main"/>
		
		<table id="dhMainContent" cellspacing="0" cellpadding="0">
		<tbody>
		<tr>
		<td class="dh-zone-box-header">
			<a href="/links"><img id="dhHeaderWeb" class="dh-header-image" src="/images2/${buildStamp}/header_linkhome.gif"/></a>
		</td>
		<td class="dh-zone-box-spacer"></td>
		<td class="dh-zone-box-header">
			<a href="/music"><img id="dhHeaderMusic" class="dh-header-image" src="/images2/${buildStamp}/header_musichome.gif"/></a>
		</td>
		<td class="dh-zone-box-spacer"></td>
		<td class="dh-zone-box-header">
			<a href="/tv"><img id="dhHeaderTv" class="dh-header-image" class="dh-header-image" src="/images2/${buildStamp}/header_tvhome.gif"/></a>
		</td>
		</tr>
		<tr>
		<td id="dhZoneBoxWeb" class="dh-zone-box dh-color-normal" valign="top">
			<div class="dh-zone-box-content">
				<dht:requireLinksGlobalBean/>
				<dht:postList posts="${linksGlobal.hotPosts.results}" format="full" separators="true" favesMode='none'/>
			</div>
		</td>
		<td></td>
		<td id="dhZoneBoxMusic" class="dh-zone-box dh-color-normal" valign="top">
			<div class="dh-zone-box-content">
				<dht:requireMusicGlobalBean/>
				<c:forEach items="${musicGlobal.recentTracks.results}" var="track" varStatus="status">
					<dht:track track="${track}" albumArt="true"/>
					<c:if test="${!status.last}">
						<dht:zoneBoxSeparator/>
					</c:if>
				</c:forEach>
			</div>
		</td>
		<td></td>
		<td id="dhZoneBoxTv" class="dh-zone-box dh-color-normal" valign="top">
			<div class="dh-zone-box-content">
				<div class="dh-item">
				<p style="color:#444444;">A work in progress, actually we're just starting on it now. But you can check out the <a href="/tv">TV Party</a> page to follow our progress and have your say if you'd like.		
				</p>
				</div>
			</div>
		</td>
		</tr>	
		<tr>
		<td class="dh-zone-box-footer">
			<img src="/images2/${buildStamp}/bottom_link230.gif" class="dh-bottom-image"/>
		</td>
		<td></td>
		<td class="dh-zone-box-footer">
			<img src="/images2/${buildStamp}/bottom_music230.gif" class="dh-bottom-image"/>
		</td>
		<td></td>
		<td class="dh-zone-box-footer">
			<img src="/images2/${buildStamp}/bottom_tvparty230.gif" class="dh-bottom-image"/>
		</td>
		</tbody>
		</table>
		<dht:footer/>
	</div>
</body>
</html>
