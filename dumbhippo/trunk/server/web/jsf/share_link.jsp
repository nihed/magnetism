<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" >

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>

<f:view>
  <c:url value="/javascripts/prototype.js" var="prototype"/>
  <c:url value="/javascripts/scriptaculous.js" var="scriptaculous"/>
  <c:url value="/javascripts/util.js" var="util"/>
  <c:url value="/javascripts/effects.js" var="effects"/>
  <c:url value="/javascripts/dragdrop.js" var="dragdrop"/>
  <c:url value="/javascripts/controls.js" var="controls"/>
  <c:url value="/javascripts/slider.js" var="slider"/>
  <c:url value="/css/share-link.css" var="pagestyle"/>
  <c:url value="/xml/people" var="peoplecompleter"/>
  <html>
	  <head>
 	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  		<title>TITLE HERE</title>
  		<script src="${prototype}" type="text/javascript"></script>
		<script src="${scriptaculous}" type="text/javascript"></script>
		<script src="${util}" type="text/javascript"></script>		
		<script src="${effects}" type="text/javascript"></script>
		<script src="${dragdrop}" type="text/javascript"></script>
		<script src="${controls}" type="text/javascript"></script>
		<script src="${slider}" type="text/javascript"></script>				
		<style type="text/css">
    div.auto_complete {
      position:absolute;
      width:250px;
      background-color:white;
      border:1px solid #888;
      margin:0px;
      padding:0px;
    }
    li.selected { background-color: #ffb; }
</style>
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

<div class="share"><input accesskey="s" class="share" type="submit" value="Share"/></div>

</form>

</div>
<br/>


<input type="text" id="autocomplete"/><div id="autocomplete_choices"></div>

<script type="text/javascript" language="javascript">
// <![CDATA[
var d = new Date();
var comp = new Ajax.Autocompleter("autocomplete", "autocomplete_choices", "${peoplecompleter}", {method: 'get', asynchronous: true});
// ]]>
</script>

      </body>
  </html>
</f:view>

