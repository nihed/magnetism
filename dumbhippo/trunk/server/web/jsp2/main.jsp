<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/main.css"/>
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
			setupRollover('dhHeaderWeb', '/images2/header_linkhome2.gif');
			setupRollover('dhHeaderMusic', '/images2/header_musichome2.gif');
			setupRollover('dhHeaderTv', '/images2/header_tvhome2.gif');
		}
	</script>
</head>
<%-- can use dht:body once we don't need onload here --%>
<body onload="init();" class="dh-gray-background-page">
	<div id="dhPage">

		<div id="dhMainLogo">
		    <div id="dhMainLogoLinks"><a class="dh-logo-link" href="/who-are-you">Log In</a> | <a class="dh-logo-link" href="/signup">Sign Up</a></div>
		</div>

		<div id="dhMainContent">
			<div class="dh-zone-box">
				<div><a href="/links"><img id="dhHeaderWeb" class="dh-header-image" src="/images2/header_linkhome.gif"/></a></div>
				<div class="dh-zone-box-border dh-color-web">
					<div class="dh-zone-box-content dh-color-normal">
						<dht:requireLinksGlobalBean/>
						<dht:postList posts="${linksGlobal.hotPosts.list}" format="full" separators="true" favesMode='none'/>
					</div>
				</div>
			</div>
			<div class="dh-zone-box-spacer"> </div>
			<div class="dh-zone-box">
				<div><a href="/music"><img id="dhHeaderMusic" class="dh-header-image" src="/images2/header_musichome.gif"/></a></div>
				<div class="dh-zone-box-border dh-color-music">
					<div class="dh-zone-box-content dh-color-normal">
						<dht:requireMusicGlobalBean/>
						<c:forEach items="${musicGlobal.recentTracks.results}" var="track" varStatus="status">
							<dht:track track="${track}" albumArt="true"/>
							<c:if test="${!status.last}">
								<dht:zoneBoxSeparator/>
							</c:if>
						</c:forEach>
					</div>
				</div>
			</div>
			<div class="dh-zone-box-spacer"> </div>
			<div class="dh-zone-box">
				<div><a href="/tv"><img id="dhHeaderTv" class="dh-header-image" class="dh-header-image" src="/images2/header_tvhome.gif"/></a></div>
				<div class="dh-zone-box-border dh-color-tv">
					<div class="dh-zone-box-content dh-color-normal">
						<div class="dh-item">
							Coming Soon
						</div>
						<dht:zoneBoxSeparator/>
						<div class="dh-item">
							We're working on this. Let us know what you think of 
							<a href="/tv">our mockups so far</a>.
						</div>
					</div>
				</div>
			</div>
			<div class="dh-grow-div-around-floats"><div></div></div>
		</div>
		<div id="dhMainColumnBottoms">
			<div id="dhMainColumnOneLeftSide" class="dh-column-side dh-color-web"><div></div></div>
			<div id="dhMainColumnOneRightSide" class="dh-column-side dh-color-web"><div></div></div>
			<div id="dhMainColumnOneTwoGap" class="dh-column-gap"><div></div></div>
			<div id="dhMainColumnTwoLeftSide" class="dh-column-side dh-color-music"><div></div></div>
			<div id="dhMainColumnTwoRightSide" class="dh-column-side dh-color-music"><div></div></div>			
			<div id="dhMainColumnTwoThreeGap" class="dh-column-gap"><div></div></div>
			<div id="dhMainColumnThreeLeftSide" class="dh-column-side dh-color-tv"><div></div></div>
			<div id="dhMainColumnThreeRightSide" class="dh-column-side dh-color-tv"><div></div></div>
			<div id="dhMainColumnOneBottom" class="dh-column-bottom"><img src="/images2/bottom_link230.gif" class="dh-bottom-image"/></div>
			<div id="dhMainColumnTwoBottom" class="dh-column-bottom"><img src="/images2/bottom_music230.gif" class="dh-bottom-image"/></div>
			<div id="dhMainColumnThreeBottom" class="dh-column-bottom"><img src="/images2/bottom_tvparty230.gif" class="dh-bottom-image"/></div>
		</div>
		<dht:footer/>
	</div>
</body>
</html>
