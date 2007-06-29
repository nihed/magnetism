<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Stacker for Apple Dashboard</title>
	<style type="text/css">
		body, td, a, p, div, span {
			font-size:	13px;
			font-family: arial, sans-serif;
		}
		body {
			background-color:	#FFFFFF;
		}
		a:link {
			color: #0000CC;
		}
	</style>
	<script type="text/javascript">
		setInterval(function() { window.open(document.location.href, "_self", null, true); },
			1000 * 60 * 10);
	</script>
</head>

<body>
	<dht3:miniStacker/>
</body>
