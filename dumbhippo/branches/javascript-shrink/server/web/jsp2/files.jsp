<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="files" class="com.dumbhippo.web.pages.PersonFilesPage" scope="request"/>
<jsp:setProperty name="files" property="viewedUserId" param="who"/>

<%-- use viewedPerson (PersonView) on most of the page, but it will throw
     if unset so here we look at viewedUser --%>
<c:if test="${empty files.viewedUser}">
	<dht:errorPage>Person not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${files.viewedPerson.name}"/>'s Files</title>
	<dht:siteStyle/>	
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<dh:script srcs="dh/fileinput.js,dh/files.js"/>
	<script type="text/javascript">
		var dhFilesInit = function() {
			new dh.fileinput.Entry(document.getElementById('dhFileUploadEntry'));
		}
		dojo.event.connect(dojo, "loaded", dj_global, "dhFilesInit");
	</script>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxAccount><%-- FIXME use proper box header --%>

			<c:choose>
			 	<c:when test="${signin.valid && files.self}">
					<dht:zoneBoxTitle>UPLOAD A FILE</dht:zoneBoxTitle>
					<div>
						<form enctype="multipart/form-data" action="/files/user" method="post">
							<input id='dhFileUploadEntry' type="file" name="file"/>
							<input type="hidden" name="reloadTo" value="/files?who=${signin.user.id}"/>
						</form>
					</div>
					<div>
						You have <c:out value="${files.quotaRemaining}"/> of space left.
					</div>
				</c:when>
				<c:otherwise>
					<%-- if you delete this text, note the page can end up starting
						with a separator --%>
					Files uploaded by <c:out value="${files.viewedPerson.name}"/>
				</c:otherwise>
			</c:choose>
					
			<c:if test="${files.publicFiles.resultCount > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle a="dhPublicFiles">PUBLIC FILES</dht:zoneBoxTitle>
				<dht:zoneBoxSubtitle>Files available to anyone on the Internet.</dht:zoneBoxSubtitle>
				<div>
					<dht:fileList files="${files.publicFiles.results}"/>
				</div>
				<dht:expandablePager pageable="${files.publicFiles}" anchor="dhPublicFiles"/>
			</c:if>

			<c:if test="${files.privateFiles.resultCount > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle a="dhPrivateFiles">PRIVATE FILES</dht:zoneBoxTitle>
				<dht:zoneBoxSubtitle>Files visible only to you.</dht:zoneBoxSubtitle>
				<div>
					<dht:fileList files="${files.privateFiles.results}"/>
				</div>
				<dht:expandablePager pageable="${files.privateFiles}" anchor="dhPrivateFiles"/>
			</c:if>

			<c:if test="${files.sharedFiles.resultCount > 0}">
				<dht:zoneBoxSeparator/>			
				<dht:zoneBoxTitle a="dhSharedFiles">MUGSHOT FILES</dht:zoneBoxTitle>
				<dht:zoneBoxSubtitle>Files available to specific people or groups on Mugshot.</dht:zoneBoxSubtitle>
				<div>
					<dht:fileList files="${files.sharedFiles.results}"/>
				</div>
				<dht:expandablePager pageable="${files.sharedFiles}" anchor="dhSharedFiles"/>
			</c:if>
			
		</dht:zoneBoxAccount>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
