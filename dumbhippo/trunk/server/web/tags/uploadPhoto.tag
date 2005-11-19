<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="location" required="true" type="java.lang.String"%>
<%@ attribute name="groupId" required="false" type="java.lang.String"%>
<%@ attribute name="linkText" required="false" type="java.lang.String"%>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhUploadPhotoCount}">
	<c:set var="dhUploadPhotoCount" value="0" scope="request"/>
</c:if>
<c:set var="dhUploadPhotoCount" value="${dhUploadPhotoCount + 1}" scope="request"/>
<c:set var="N" value="${dhUploadPhotoCount}" scope="page"/>

<c:url value="/upload${location}" var="posturl"/>
<form id="dhPhotoUploadForm${N}" enctype="multipart/form-data" action="${posturl}" method="post">
	<c:if test="${!empty linkText}">
		<c:set var="entryclass" value="dhInvisible" scope="request"/>
		<a id="dhChangePhotoLink${N}" href="javascript:void(0);" onClick="dh.util.hideId('dhChangePhotoLink${N}');dh.util.showId('dhPhotoUploadFileEntry${N}');">${linkText}</a>
	</c:if>
	<input class="${entryclass}" onChange="document.forms['dhPhotoUploadForm${N}'].submit();" id="dhPhotoUploadFileEntry${N}" type="file" name="photo"/>
	<!-- we just always submit this, servlet ignores it if we aren't 
		changing a group photo -->
	<input type="hidden" name="groupId" value="${groupId}"/>
</form>
