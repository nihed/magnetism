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
        <%-- because the list is sorted, current set will be selected, since it will show up first in the list --%>
        <c:forEach items="${statistics.fileOptions}" var="filename">
	        <option><c:out value="${filename}"/></option>				    
        </c:forEach>
	</select>
    <table>
    <tr>     
    <td colspan="2">
        <select id="dhColumnSelect" onchange="dh.statistics.onSelectedColumnChange();">
	    </select>
    </td>
    <td colspan="1">
        <select id="dhServerSelect" onchange="dh.statistics.onSelectedServerChange();">
	    </select>
    </td>
    </tr>
    <tr>
    <td width="60px;" valign="top" align="right">
        <div id="dhMaxVal"><span/></div>
    </td> 
    <td colspan="2" rowspan="2">
	    <div id="dhGraph1" class="dh-graph">
        </div>
    </td> 
    </tr>
    <tr>
    <td width="60px;" valign="bottom" align="right">     
        <div id="dhMinVal"><span/></div>
    </td>
    </tr>
    <tr>
    <td>
    </td>
    <td>
        <div id="dhStartTime"><span/></div>
    </td>
    <td align="right">
        <div id="dhEndTime"><span/></div>
    </td>
    </tr>
    <tr>
    <td>
    </td>
    <td colspan="2" align="center">
	    <div id="dhCoords1" class="dh-coordinates">
	        <span/>
        </div>
    </td> 
    </table>
    <%-- <div id="dhHourSelector"></div> --%>
</body>
</html>
