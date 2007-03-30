<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="request"/>
<dh:bean id="application" class="com.dumbhippo.web.pages.ApplicationPage" scope="page"/>
<jsp:setProperty name="application" property="applicationId" param="id"/>

<c:if test="${empty application.application || application.application.application.deleted}">
	<dht:errorPage>Application not found</dht:errorPage>
</c:if>

<c:set var="appinfo" value="${application.appinfoFile}"/>

<head>
	<title>Edit Application - <c:out value="${appinfo.name}"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
	<dh:script module="dh.util"/>
	<script type="text/javascript">
	dhIconCounter = 0;
	dhIconSizes = ["unspecified", "16x16", "22x22", "24x24", "32x32", "48x48", "64x64", "128x128", "scalable"];
	function dhAddIcon() {
		document.getElementById("dhAddIconHeader").style.display = ""; // IE compat hack
		var row = document.createElement("tr");
		var base = "icon" + ++dhIconCounter;

		var cell = document.createElement("td");
		row.appendChild(cell);		
		var input = document.createElement("input");
		cell.appendChild(input);
		input.type = "file";
		input.size = 40;
		input.name = base + "-file";

		cell = document.createElement("td");
		row.appendChild(cell);		
		input = document.createElement("input");
		cell.appendChild(input);
		input.name = base + "-theme";

		cell = document.createElement("td");
		row.appendChild(cell);		
		var select = document.createElement("select");
		cell.appendChild(select);
		select.name = base + "-size";
		for (var i = 0; i < dhIconSizes.length; i++){
			select.add(new Option(dhIconSizes[i], dhIconSizes[i]), null);
		}
		
		document.getElementById("dhAddIconBody").appendChild(row);
	}
	function dhOnComment1Change() {
		var comment1 = document.getElementById("dhApplicationComment1");
		var comment2 = document.getElementById("dhApplicationComment2");
		
		comment2.value = comment1.value;
	}
	function dhOnComment2Change() {
		var comment1 = document.getElementById("dhApplicationComment1");
		var comment2 = document.getElementById("dhApplicationComment2");
		
		comment1.value = comment2.value;
	}
	function dhApplicationSave() {
		var comment = document.getElementById("dhApplicationComment1");
		var commentValue = dh.util.trim(comment.value);
		if (commentValue == "") {
			alert("Please enter a comment describing the changes you are making");
			return;
		}
		document.getElementById("dhApplicationEditForm").submit();
	}
	</script>
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
		<div class="dh-page-shinybox-title-large">Edit Application - <c:out value="${appinfo.name}"/></div>
		<div>
   			This page allow allows you to edit the application database information for <c:out value="${appinfo.name}"/>.
			<a href="/application-history?id=${appinfo.appId}">View history</a> |
			<a href="/application?id=${appinfo.appId}">Go back to browsing</a>
		</div>
	    <hr>
	    <div>
	    	<form id="dhApplicationEditForm" action="/upload/appinfo-edit" method="POST" enctype="multipart/form-data">
		    	<h3>Basic Application Information</h3>
	    		<input type="hidden" name="appId" value="${appinfo.appId}"/>
		    	<table class="dh-application-edit">
		    		<dht3:applicationEditRow id="dhApplicationName" name="name" label="Name" value="${appinfo.name}">
		    			<jsp:attribute name="help">
		    				Name displayed in the user interface.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationGenericName" name="genericName" label="Generic Name" value="${appinfo.genericName}">
		    			<jsp:attribute name="help">
		    				Generic type of this application (Web Browser).
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationTooltip" name="tooltip" label="Tooltip" value="${appinfo.tooltip}">
		    			<jsp:attribute name="help">
							Short description of this application (Browse the Web).
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationDescription" name="description" label="Description" value="${appinfo.description}" multiline="true">
		    			<jsp:attribute name="help">
		    				Detailed description of the application in a paragraph.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationCategory" name="category" label="Category">
		    			<jsp:attribute name="help">
		    				Category that this application belongs to.
		    			</jsp:attribute>
		    			<jsp:attribute name="contents">
		    				<select name="category" id="dhApplicationCategory">
		    					<c:forEach items="${application.categories}" var="category">
		    						<c:choose>
		    							<c:when test="${category == appinfo.category}">
					    					<option value="${category.name}" selected="1"><c:out value="${category.displayName}"/></option>
					    				</c:when>
					    				<c:otherwise>
					    					<option value="${category.name}"><c:out value="${category.displayName}"/></option>
					    				</c:otherwise>
					    			</c:choose>
		    					</c:forEach>
		    				</select>
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<tr>
	    			<td class="dh-application-edit-label">
	    			Icons:
	    			</td>
	    			<td>
	    				<c:if test="${dh:size(appinfo.icons) > 0}">
					    	<table>
					    		<tr>
	    						<th>
					    		</th>
				    			<th>
					    			Theme
					    		</th>
					    		<th>
					    			Size
					    		</th>
					    		<th>
				    				Delete
				    			</th>
					    		</tr>
						    	<c:forEach items="${appinfo.icons}" var="icon">
						    		<tr>
						    		<td>
						    			<dht3:appinfoIcon upload="${application.upload}" icon="${icon}"/>
							    	</td>
					    			<td>
				    					<c:out value="${icon.theme}"/>
					    			</td>
					    			<td>
					    				<c:out value="${icon.size}"/>
					    				</td>
			    					<td>
			    						<input type="checkbox" name="delete-icon${empty icon.theme ? '' : '.'}${icon.theme}${empty icon.size ? '' : '.'}${icon.size}"/>
			    					</td>
						    		</tr>
				    			</c:forEach>
	    					</table>
	    				</c:if>
	    				<table>
				    	<tbody id="dhAddIconBody">
				    	<tr id="dhAddIconHeader" style="display: none;">
			    		<th>
	   						File
				   		</th>
				   		<th>
	   						Theme
				   		</th>
				   		<th>
	   						Size
				   		</th>
	    				</tr>
					    </tbody>
		    			</table>
					    <a href="javascript:dhAddIcon()">Add an icon</a>
					</td>
					</tr>
		    		<tr class="dh-application-edit-spacer-row"></tr>
		    		<dht3:applicationEditRow id="dhApplicationComment1" name="comment" label="Comment" value="" multiline="true" onchange="dhOnComment1Change()" rowClass="dh-application-edit-comment">
		    			<jsp:attribute name="help">
		    				Describe the changes you are making
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<tr>
		    			<td></td>
		    			<td class="dh-application-edit-save"><input type="button" value="Save" onclick="dhApplicationSave()"></input></td>
		    		</tr>
		    	</table>
		    	<hr/>
		    	<h3>Identifying, Installing, and Launching</h3>
		    	<table class="dh-application-edit">
		    		<dht3:applicationEditRow id="dhApplicationWmClasses" name="wmClasses" label="WM Classes" value="${appinfo.wmClassesString}">
		    			<jsp:attribute name="help">
							List of window class names that might be found on a window for this application (;&nbsp;separated). 
							You can find the window class for an application by looking at the output of 'xprop WM_CLASS'.
							The appropriate value is the second of the two elements.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationTitlePatterns" name="titlePatterns" label="Title Patterns" value="${appinfo.titlePatternsString}">
		    			<jsp:attribute name="help">
							Generally not needed. If this application shares the same WM class as other
							applications, provide regular expressions here that will be downloaded 
							to all clients and be matched against window titles (;&nbsp;separated).
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationDesktopNames" name="desktopNames" label="Desktop Names" value="${appinfo.desktopNamesString}">
		    			<jsp:attribute name="help">
		    				Names to look under when finding a desktop file to launch this application. (; separated)
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationPackageNames" name="packageNames" label="Package Names" value="${appinfo.packageNames}">
		    			<jsp:attribute name="help">
		    				Packages in which this application is included. (Of the form "&lt;distribution1&gt;=&lt;package1&gt;;&lt;distribution&gt;=&lt;package2&gt;;...")
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<tr class="dh-application-edit-spacer-row"></tr>
		    		<dht3:applicationEditRow id="dhApplicationComment2" name="comment2" label="Comment" value="" multiline="true" onchange="dhOnComment2Change()" rowClass="dh-application-edit-comment">
		    			<jsp:attribute name="help">
		    				Describe the changes you are making
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<tr>
		    			<td></td>
		    			<td class="dh-application-edit-save"><input type="button" value="Save" onclick="dhApplicationSave()"></input></td>
		    		</tr>
		    	</table>
	    	</form>
	    </div>
	</dht3:shinyBox>
</dht3:page>
