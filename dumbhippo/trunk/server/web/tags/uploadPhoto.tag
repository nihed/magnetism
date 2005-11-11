<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="location" required="true" type="java.lang.String"%>
<c:url value="/upload${location}" var="posturl"/>
<form id="dh-photo-upload-form" enctype="multipart/form-data" action="${posturl}" method="post">
	<input id="dhPhotoUploadFileEntry" type="file" name="photo"/>
	<br/>
	<input id="dhPhotoUploadSubmitButton" type="submit" value="Do it!"/>
</form>
