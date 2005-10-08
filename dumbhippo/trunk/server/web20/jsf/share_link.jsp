<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link rel="stylesheet" href="../css/share-link.css" type="text/css" />
<title>Share Link</title>
</head>
<body>

<div class="share-link">

<strong>Share Link</strong>

<div class="url">#{ url }</div>
<form>

<input type="hidden" name="url" value="#{ url }" />

<div class="recipients"><div class="label">Share <u>W</u>ith:</div>
	<!-- needs gmail auto-completion -->
	<input accesskey="w" type="text" class="recipients" value="#{ recipients }"/>
	<div class="recipient-list">[As We Recognize Names, We Drop Them Here]</div>
</div>

<div class="description"><div class="label"><u>D</u>escription:</div>
	<!-- mimick google talk ui and expand as they type more -->
	<textarea accesskey="d" rows="1" class="description">#{ description }</textarea>
</div>

<div class="share"><input accesskey="s" class="share" type="submit" value="Share" /></div>

</form>

</div>

</body>
</html>