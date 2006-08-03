<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

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
        <option>limpopo.dumbhippo.com - 2006--06-10</option>
	</select>
    <table>
    <tr>
    <td>
        <select>
	    <option>Database Connections</option>
	    <option>Memory Used</option>
	    <option>Pages served</option>
	</select>
    </td>
    <td align="right">
        <select>
	    <option>Average</option>
	    <option>Cumulative</option>
	</select>
    </td>
    <td align="right" style="width: 60px;" >
        <input type="button" value="Remove" style="width: 100%"></input
	</td>
    </tr>
    <tr>
    <td colspan="3">
	    <div id="dhGraph1" class="dh-graph">
			<div style="position: absolute; top: 3px; right: 8px; text-align: center; font-size: 18px;">
		        <div>+</div>
   			    <div>-</div>
		    </div>
        </div>
    </td> 
    </tr>
    <tr>
    <td>
        <select>
	    <option>Pages served</option>
	    <option>Database Connections</option>
	    <option>Memory Used</option>
	</select>
    </td>
    <td align="right">
        <select>
	    <option>Cumulative</option>
	    <option>Average</option>
	</select>
    </td>
    <td align="right" style="width: 60px;" >
        <input type="button" value="Remove" style="width: 100%"></input
	</td>
    </tr>
    <tr>
    <td colspan="3">
	    <div id="dhGraph2" class="dh-graph">
			<div style="position: absolute; top: 3px; right: 8px; text-align: center; font-size: 18px;">
		        <div>+</div>
   			    <div>-</div>
		    </div>
        </div>
    </td> 
    </tr>
    <tr>
    <td>
        <select>
	    <option>---</option>
	        <option>Pages served</option>
	        <option>Database Connections</option>
	        <option>Memory Used</option>
   	    </select>
    </td>
    <td align="right">
        <select disabled>
	        <option>Average</option>
	        <option>Cumulative</option>
		</select>
    </td>
    <td align="right" style="width: 60px;">
        <input type="button" value="Remove" style="width: 100%" disabled></input
	</td>
    </td>
    </tr>
    </table>
    <div id="dhHourSelector"></div>
</body>
</html>
