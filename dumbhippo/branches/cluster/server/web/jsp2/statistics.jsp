<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="statistics" class="com.dumbhippo.web.pages.StatisticsPage" scope="page"/>

<head>
	<title>Mugshot Server Statistics</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/statistics.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript" src="javascript/dh/statistics/set.js"></script>
	<script type="text/javascript" src="javascript/dh/statistics/fetcher.js"></script>
	<script type="text/javascript" src="javascript/dh/statistics/block.js"></script>
	<script type="text/javascript" src="javascript/dh/statistics.js"></script>
	<script type="text/javascript">
//		dojo.require("dh.statistics");
		dojo.event.connect(dojo, "loaded", dj_global, "dhStatisticsInit");
		dh.statistics.servers = [ <c:forEach items="${statistics.servers}" var="server">
		    <dh:jsString value="${server}"/>,
		</c:forEach> ];
		dh.statistics.thisServer = <dh:jsString value="${statistics.thisServer}"/>;
	</script>
</head>
<body>
    <select id="dhFileSelect" onchange="dh.statistics.onSelectedFileChange();">
        <c:forEach items="${statistics.sets}" var="set">
	        <option value="${set.filename}"><c:out value="${set.name}"/></option>				    
        </c:forEach>
	</select>
	<div id="dhBlocksDiv">
	</div>
	<input type="button" value="Add" onclick="dh.statistics.addBlock();"></input>
</body>
</html>
