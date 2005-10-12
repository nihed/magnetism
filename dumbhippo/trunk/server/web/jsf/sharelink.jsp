<html>
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
  <c:url value="/css/sharelink.css" var="pagestyle"/>
  <c:url value="/xml/friendcompletions" var="xmlfriendcompletions"/>
  
	  <head>
 	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  		<title>Share a Link</title>
  		<script src="${prototype}" type="text/javascript"></script>
		<script src="${scriptaculous}" type="text/javascript"></script>
		<script src="${util}" type="text/javascript"></script>		
		<script src="${effects}" type="text/javascript"></script>
		<script src="${dragdrop}" type="text/javascript"></script>
		<script src="${controls}" type="text/javascript"></script>
		<script src="${slider}" type="text/javascript"></script>
		
		<link rel="stylesheet" href="${pagestyle}" type="text/css"></style>
		<link rel="stylesheet" href="${autocompletestyle}" type="text/css"></style>
		
      </head>
      <body>

<div class="share-link">

<strong>Share Link</strong>

<div class="url">#{ sharelink.url }</div>
<h:form>

<div class="recipients"><div class="label">Share <u>W</u>ith:</div>
<input accesskey="w" type="text" id="friendentry" class="autocomplete" value="#{ recipients }"/>
<div id="friendentry-choices" class="autocomplete-choices"></div>

<script type="text/javascript" language="javascript">
// <![CDATA[
new Ajax.Autocompleter("friendentry", "friendentry-choices", "${xmlfriendcompletions}", {method: 'get', asynchronous: true});
// ]]>
</script>

<div class="recipient-list">[As We Recognize Names, We Drop Them Here]</div>
</div>

<div class="description"><div class="label"><u>D</u>escription:</div>
	<!-- FIXME mimick google talk ui and expand as they type more -->
	<h:inputTextarea accesskey="d" rows="2" styleClass="description" value="#{ sharelink.description }"/>
</div>

<div class="share"><h:commandButton accesskey="s" styleClass="share" value="Share" action="#{ sharelink.doShareLink }"/></div>
</h:form>

</div><!-- class=share-link -->

      </body>
</f:view>
</html>

