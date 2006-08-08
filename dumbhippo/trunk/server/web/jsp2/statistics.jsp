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
	<script type="text/javascript">
		dojo.require("dh.statistics");
		dojo.event.connect(dojo, "loaded", dj_global, "dhStatisticsInit");
	</script>
</head>
<body>
    <select>
        <option><c:out value="${statistics.fileOption}"/></option>
	</select>
    <table>
    <tr>     
    <td colspan="3">
        <select id="dhColumnSelect" onchange="dh.statistics.onSelectedColumnChange();">
            <c:forEach items="${statistics.currentSet.columns.columns}" var="column">
	            <option id="${column.id}">
	                <c:out value="${column.name} -- ${column.units} -- ${column.type}"/>
	            </option>				    
			</c:forEach>
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
    </table>
    <%-- <div id="dhHourSelector"></div> --%>
</body>
</html>
