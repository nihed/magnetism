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
			text-align:			left;
			/* for opacity to work in IE you have to set width or height, or use position absolute */
			width:				480px;
		}
		.dh-stacked-block .dh-heading {
			position:			relative;
			font-size:			11px;
			margin:				2px;
		}
		.dh-stacked-block .dh-title {
			font-size:			11px;
		}
		.dh-stacked-block .dh-content-padding {
			position:			relative;
			width:				480px;			
		}
		.dh-stacked-block .dh-content {
			position:			relative;
			margin:				7px;
			width:				466px;
		}
		.dh-stacked-block .dh-content-padding {
			background-color: 	white;
			color:				black;	
		}		
		.dh-stacked-block .dh-heading a:visited, .dh-stacked-block .dh-heading a {
			color:				white;
		}
		.dh-stacked-block .dh-hush {
			position:			absolute;
			right:				2px;
			top:				2px;
			font-weight:		bold;
			font-size:			10px;
			background-color: 	inherit; /* so it covers up any text below it */
			cursor:			pointer;
		}
		.dh-stacked-block-music-person {
			background-color:	#36AAE8;
			color:				white;		
		}
		.dh-stacked-block-post {
			background-color:	#F16D1C;
			color:				white;
		}
		.dh-stacked-block-group-chat {
			background-color:	#84569B;
			color:				white;	
		}
		.dh-stacked-block-group-member {
			background-color:	#84569B;
			color:				white;	
		}	
		.dh-stacked-block .dh-left-column {
			position:	relative;
			width: 		350px;
		}
		.dh-stacked-block .dh-right-column {
			position:	absolute;
			top:		0px;
			right:		0px;
			width:		100px;
		}
		.dh-stacked-block .dh-right-column .dh-from {
			position:	relative;
			width:		100px;
			text-align:	center;
			font-size:	10px;
			font-style: italic;
		}
		.dh-stacked-block .dh-right-column .dh-details {
			position:	relative;
			width:		100px;
			font-size:	10px;
		}
		.dh-stacked-block .dh-right-column .dh-details .dh-pipe {
			margin-left:	3px;
			margin-right:	3px;
		}
		.dh-stacked-block .dh-right-column .dh-details div {
			float:		right;
		}
		.dh-stacked-block .dh-left-column .dh-description {
			font-size: 	10px;
		}
		.dh-stacked-block .dh-left-column .dh-messages {
			font-size: 	10px;
			font-style: italic;
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
