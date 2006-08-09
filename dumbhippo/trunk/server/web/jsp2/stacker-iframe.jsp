<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Stacker</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script src="/javascript/${buildStamp}/dh/stacker.js" type="text/javascript"></script>
	<style type="text/css">
		body {
			/* firefox sets a default body margin of 8px */
			margin:				0px;
		}
		#dhStackerBackground {
			position:			relative;
			background-color:	white;
			overflow:			hidden;
			margin:				0px;
			padding:			0px;
		}
		.dh-stacked-block-outer {
			position:			relative;		
			/* for opacity to work in IE you have to set width or height, or use position absolute */
			width:				480px;
			overflow:			hidden;
			/* don't add a margin here or it makes the animation ugly when blocks move 
			 * since we modify the height of this block in that animation but not the margin
			 */
		}		
		.dh-stacked-block {
			position:			relative;
			background-color:	blue;
			color:				white;
			text-align:			left;
			/* for opacity to work in IE you have to set width or height, or use position absolute */
			width:				480px;
		}
		.dh-stacked-block .dh-title {
			position:			relative;
		}
		.dh-stacked-block .dh-content {
			position:			relative;
			background-color: 	aqua;
			margin:				1px;
		}
	</style>
	<script>
		dojo.require('dh.stacker');
		var dhStackerInit = function() {
			var stacker = dh.stacker.getInstance();
			stacker.setContainer(document.getElementById('dhStackerBackground'));
			stacker.start();
		}
	</script>
</head>
<body onload="dhStackerInit();">
	<div id="dhStackerBackground">
	</div>
</body>
</html>
