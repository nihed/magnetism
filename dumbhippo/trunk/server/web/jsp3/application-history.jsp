<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="request"/>
<dh:bean id="application" class="com.dumbhippo.web.pages.ApplicationPage" scope="page"/>
<jsp:setProperty name="application" property="applicationId" param="id"/>
<jsp:setProperty name="application" property="uploadId" param="version"/>

<c:if test="${empty application.application || application.application.application.deleted}">
	<dht:errorPage>Application not found</dht:errorPage>
</c:if>

<c:set var="appView" value="${application.application}"/>
<c:set var="appinfo" value="${application.appinfoFile}"/>

<head>
	<title>Application History - <c:out value="${appinfo.name}"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
    <dh:script modules="dh.util,dh.server"/>
	<script type="text/javascript">
	function dhRevertApplication() {
		var applicationId = <dh:jsString value="${appView.application.id}"/>;
		var uploadId = <dh:jsString value="${application.upload.upload.id}"/>;
		var comment = dh.util.trim(document.getElementById('dhApplicationRevertComment').value);
		if (comment == '') {
			alert("Please enter a comment describing your reason for reverting");
			return;
		}
		
		dh.server.doXmlMethod("revertapplication",
        	{ "applicationId" : applicationId,
        	  "version" : uploadId,
        	  "comment" : comment },
            function(type, data, http) {
            	document.location.href = "/application-history?id=" + applicationId;
            },
            function(code, msg) {
                 alert("Failed to revert to previous version: " + msg);
			});
	}
	</script>
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
		<div class="dh-page-shinybox-title-large">Application History - <c:out value="${appinfo.name}"/></div>
		<div>
   			This page shows past versions of application information for <c:out value="${appinfo.name}"/>.
			<a href="/application-edit?id=${appinfo.appId}">Edit current</a> |
			<a href="/application?id=${appinfo.appId}">Go back to browsing</a>
		</div>
	    <hr>
	    <div id="dhApplicationsRight">
	    	<table id="dhApplicationEdit">
	    		<dht3:applicationHistoryRow id="dhApplicationUploadDate" label="Date">
	    			<jsp:attribute name="value">
						<fmt:formatDate value="${application.upload.upload.uploadDate}" pattern="yyyy-MM-dd HH:mm:ss ZZZ"/>
						<c:if test="${application.upload.current}">
							(current)
						</c:if>
	    			</jsp:attribute>
	    		</dht3:applicationHistoryRow>
	    		<dht3:applicationHistoryRow id="dhApplicationUploader" label="Uploader">
	    			<jsp:attribute name="contents">
						<a href="${application.upload.uploader.homeUrl}"><c:out value="${application.upload.uploader.name}"/></a>
	    			</jsp:attribute>
	    		</dht3:applicationHistoryRow>
	    		<tr class="dh-application-edit-spacer-row"></tr>
	    		<dht3:applicationHistoryRow id="dhApplicationName" label="Name" value="${appinfo.name}"/>
	    		<dht3:applicationHistoryRow id="dhApplicationDescription" label="Description" value="${appinfo.description}"/>
	    		<dht3:applicationHistoryRow id="dhApplicationWmClasses" label="WM Classes" value="${appinfo.wmClassesString}"/>
	    		<dht3:applicationHistoryRow id="dhApplicationTitlePatterns" label="Title Patterns" value="${appinfo.titlePatternsString}"/>
	    		<dht3:applicationHistoryRow id="dhApplicationDesktopNames" label="Desktop Names" value="${appinfo.desktopNamesString}"/>
	    		<dht3:applicationHistoryRow id="dhApplicationCategories" label="Categories" value="${appinfo.categoriesString}"/>
	    		<tr>
    			<td class="dh-application-edit-label">
    			Icons:
    			</td>
    			<td>
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
					    		</tr>
		    			</c:forEach>
    				</table>
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
				</td>
				</tr>
	    	</table>
	    </div>
	    <div class="dh-grow-div-around-floats"></div>
	    <hr/>
		<div class="dh-page-shinybox-subtitle">History</div>
		<table>
			<c:forEach items="${application.uploadHistory}" var="upload">
				<tr>
				<td>
					<fmt:formatDate value="${upload.upload.uploadDate}" pattern="yyyy-MM-dd HH:mm:ss ZZZ"/>
				</td>
				<td>
					<a href="${upload.uploader.homeUrl}"><c:out value="${upload.uploader.name}"/></a>
				</td>
				<td>
					<a href="/application-history?id=${appView.application.id}&version=${upload.upload.id}">Go</a>
				</td>
				</tr>
			</c:forEach>
		</table>
		<c:if test="${!application.upload.current}">
			<hr/>
			<div class="dh-page-shinybox-subtitle">Revert to previous version</div>
			<div>
				You can revert to the version of the application information displayed above 
				(uploaded by <c:out value="${application.upload.uploader.name}"/> 
				on <fmt:formatDate value="${application.upload.upload.uploadDate}" pattern="yyyy-MM-dd"/> at 
				<fmt:formatDate value="${application.upload.upload.uploadDate}" pattern="HH:mm:ss ZZZ"/>)
			</div>
			<div>
				<label for="dhApplicationRevertComment">Revert comment:</label>
				<br/>
				<textarea id="dhApplicationRevertComment" cols="60" rows="4"></textarea>
				<br/>
				<input type="button" value="Revert" onclick="dhRevertApplication()"/>
			</div>
		</c:if>
	</dht3:shinyBox>
</dht3:page>
