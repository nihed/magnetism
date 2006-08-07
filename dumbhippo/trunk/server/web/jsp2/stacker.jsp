<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Happenings</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<style type="text/css">
		#dhPage {
			height:		100%;
			width:		500px;
		}
		#dhStackerFrame {
			position:	relative;
			width:		500px;
			margin:		0px;
			border:		0px;
			height:		100%;
			overflow-x: hidden;
		}
		#dhStackerFrameContainer {
			position:			relative;
			width:				500px;
			background-color:	orange;
			height:				100%;
		}
		#dhStackerFrameTable {
			position:			relative;
			width:				500px;
			background-color:	blue;
			height:				100%;
		}
		#dhStackerFrameRow {
			position:			relative;
			background-color:	purple;
		}
		#dhDashboardRow {
			height:				40px;
			background-color:	purple;
			color:				white;
		}
		#dhDebugButtons {
			position:			absolute;
			right:				0px;
			top:				0px;
		}
	</style>
	<script>
		var dhGetStackerScope = function() {
			var iframe = document.getElementById('dhStackerFrame');
			return iframe.contentWindow;
		}
		var dhSimulateNewPost = function() {
			// behold the javascript scariness
			with (dhGetStackerScope()) {
				dh.stacker.simulateNewPost(dh.stacker.getInstance(), "Yay! New Post!");
			}
		}
		var dhSimulateMoreViews = function() {
			with (dhGetStackerScope()) {
				dh.stacker.simulateMoreViews(dh.stacker.getInstance());
			}
		}
		var dhSimulateNewStackTime = function() {
			with (dhGetStackerScope()) {
				dh.stacker.simulateNewStackTime(dh.stacker.getInstance());
			}
		}
	</script>	
</head>
<body class="dh-body-without-sidebar">
	<div id="dhDebugButtons">
		<div>
			<input type="button" onclick="dhSimulateNewPost()" value="new post"/>
			<input type="button" onclick="dhSimulateMoreViews()" value="more views"/>
			<input type="button" onclick="dhSimulateNewStackTime()" value="new activity"/>
		</div>
	</div>
	<div id="dhPage">
		<table id="dhStackerFrameTable" cellpadding="0" cellspacing="0">
			<tr id="dhStackerFrameRow">
				<td>
					<div id="dhStackerFrameContainer">
						<iframe id="dhStackerFrame" src="/stacker-iframe" frameborder="0" scrolling="yes">
							<div>Your web browser doesn't seem to work with this page. <a href="mailto:feedback@mugshot.org">Let us know!</a></div>
						</iframe>
					</div>
				</td>
			</tr>
			<tr id="dhDashboardRow">
				<td>
					<div id="dhDashboardContainer">
						<div class="dh-title">
							MUGSHOT STACKER
						</div>
						<div class="dh-content">
							FIND STACK ENJOY
						</div>
					</div>
				</td>
			</tr>
		</table>
	</div>
</body>
</html>
