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
	    <div id="dhApplicationsRight">
	    	<form action="/upload/appinfo-edit" method="POST" enctype="multipart/form-data">
	    		<input type="hidden" name="appId" value="${appinfo.appId}"/>
		    	<table id="dhApplicationEdit">
		    		<dht3:applicationEditRow id="dhApplicationName" name="name" label="Name" value="${appinfo.name}">
		    			<jsp:attribute name="help">
		    				Name displayed in the user interface.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationDescription" name="description" label="Description" value="${appinfo.description}" multiline="true">
		    			<jsp:attribute name="help">
		    				Detailed description of the application (one or two sentences).
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationWmClasses" name="wmClasses" label="WM Classes" value="${appinfo.wmClassesString}">
		    			<jsp:attribute name="help">
							List of WM class names that might be found on a window for this application (; separated)
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationTitlePatterns" name="titlePatterns" label="Title Patterns" value="${appinfo.titlePatternsString}">
		    			<jsp:attribute name="help">
							Regular expressions to match window titles and identify this application (; separated. generally should be empty unless multiple applications share the same window class.)
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationDesktopNames" name="desktopNames" label="Desktop Names" value="${appinfo.desktopNamesString}">
		    			<jsp:attribute name="help">
		    				Names used when finding a desktop file to launch this application. (; separated)
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationCategories" name="categories" label="Categories" value="${appinfo.categoriesString}">
		    			<jsp:attribute name="help">
		    				Categories that this application belongs to. (; separated)
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
		    		<dht3:applicationEditRow id="dhApplicationDescription" name="comment" label="Comment" value="" multiline="true">
		    			<jsp:attribute name="help">
		    				Comment describing of the change you are making
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<tr>
		    			<td></td>
		    			<td class="dh-application-edit-save"><input type="submit" value="Save"></input></td>
		    		</tr>
		    	</table>
	    	</form>
	    </div>
	</dht3:shinyBox>
</dht3:page>
