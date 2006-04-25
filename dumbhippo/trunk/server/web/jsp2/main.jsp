<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/main.css"/>
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
<body onload="init();">
	<div id="dhPage">
		<div id="dhMainTopBox">
			<div id="dhMainLogo"><img src="/images2/logotag.gif"/></div>
			<table id="dhMainLargeIntro">
				<tbody>
					<tr>
						<td>
							<div>Name tags don't tell people what you're about. Show the world what you're sharing, 
							listening to, and watching. Let them know the real you.
							</div>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
		<div id="dhMainMiddleBox">
			Admit it, you share links and listen to tunes at work or school. Real multi-taskers also 
			watch TV. Use that anti-productiveness more productively. Display your music playlist as a
			mood flag. Discuss sites and shows with friends you can't meet at the water cooler. See what
			other people are doing when they should be working. <a href="/signup">Sign&nbsp;up</a> or
			<a href="/who-are-you">Log&nbsp;in</a> to <b>Mugshot</b> and get busy!
		</div>
		<div id="dhMainContent">
			<div class="dh-zone-box">
				<div><a href="/links"><img id="dhHeaderWeb" class="dh-header-image" src="/images2/header_linkhome1.gif"/></a></div>
				<div class="dh-zone-box-border dh-color-web">
					<div class="dh-zone-box-content dh-color-normal">
						<dht:requireLinksGlobalBean/>
						<dht:postList posts="${linksGlobal.hotPosts.list}" format="full" separators="true" favesMode='none'/>
					</div>
				</div>
			</div>
			<div class="dh-zone-box-spacer"> </div>
			<div class="dh-zone-box">
				<div><a href="/music"><img id="dhHeaderMusic" src="/images2/header_musichome1.gif"/></a></div>
				<div class="dh-zone-box-border dh-color-music">
					<div class="dh-zone-box-content dh-color-normal">
						<div class="dh-item">
							<div class="dh-album-art"><div></div></div>
							<div class="dh-song-item-details">
								<div class="dh-attribution"><a href="" class="dh-name-link">Samiam</a> is playing</div>
								<div class="dh-title"><a href="">Prep Gwarlek 38 Remix</a></div>
								<div class="dh-extra-info">by <a href="" class="dh-name-link">Alarm Will Sound</a></div>
								<div class="dh-play-at">Play at <a href="">iTunes</a> | <a href="">Yahoo!</a></div>
							</div>
						</div>
						<dht:zoneBoxSeparator/>
						<div class="dh-item">
							<div class="dh-album-art"><div></div></div>
							<div class="dh-song-item-details">
								<div class="dh-attribution"><a href="" class="dh-name-link">Jon The Master</a> is playing</div>
								<div class="dh-title"><a href="">Thank You Falletinme Be Mice Elf Agin</a></div>
								<div class="dh-extra-info">by <a href="" class="dh-name-link">Sly and the Family Stone</a></div>
								<div class="dh-play-at">Play at <a href="">iTunes</a> | <a href="">Rhapsody</a> | <a href="">Yahoo!</a></div>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="dh-zone-box-spacer"> </div>
			<div class="dh-zone-box">
				<div><a href="/tv"><img id="dhHeaderTv" class="dh-header-image" class="dh-header-image" src="/images2/header_tvhome1.gif"/></a></div>
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
		</div>
		<div id="dhMainColumnBottoms">
			<div id="dhMainColumnOneLeftSide" class="dh-column-side dh-color-web"><div></div></div>
			<div id="dhMainColumnOneRightSide" class="dh-column-side dh-color-web"><div></div></div>
			<div id="dhMainColumnTwoLeftSide" class="dh-column-side dh-color-music"><div></div></div>
			<div id="dhMainColumnTwoRightSide" class="dh-column-side dh-color-music"><div></div></div>			
			<div id="dhMainColumnThreeLeftSide" class="dh-column-side dh-color-tv"><div></div></div>
			<div id="dhMainColumnThreeRightSide" class="dh-column-side dh-color-tv"><div></div></div>
			<div id="dhMainColumnOneBottom" class="dh-column-bottom"><img src="/images2/bottom_link230.gif"/></div>
			<div id="dhMainColumnTwoBottom" class="dh-column-bottom"><img src="/images2/bottom_music230.gif"/></div>
			<div id="dhMainColumnThreeBottom" class="dh-column-bottom"><img src="/images2/bottom_tvparty230.gif"/></div>
		</div>
		<dht:footer/>
	</div>
</body>
</html>
