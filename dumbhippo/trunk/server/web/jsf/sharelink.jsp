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
  <c:url value="/javascripts/sitewide.js" var="sitewide"/>
  <c:url value="/css/sharelink.css" var="pagestyle"/>
  <c:url value="/xml/friendcompletions" var="xmlfriendcompletions"/>
  <c:url value="/jsf/addclient.jsp" var="login"/>
  
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
	    <script src="${sitewide}" type="text/javascript"></script>
		
		<link rel="stylesheet" href="${pagestyle}" type="text/css"></style>
		
<script type="text/javascript" language="javascript">
// <![CDATA[
function toCommaString(vals) {
   var commaString = vals.join();
   return commaString;
}
// this parsing is not just split() since we want to handle user-inputted comma strings
function fromCommaString(commaString) {
   var vals = new Array();
   var split = commaString.split(",");
   for (var i = 0; i < split.length; ++i) {
       var r = split[i];
	   r = trimWhitespace(r);
       if (r.length > 0) {
           vals.push(r);
       }
   }
   return vals;
}
function arrayContains(a, val) {
   for (var i = 0; i < a.length; ++i) {
       if (a[i] == val)
           return true;
   }
   return false;
}
// yay inefficiency
function mergePreservingOrder(merged, a) {
   for (var i = 0; i < a.length; ++i) {
 		if (!arrayContains(merged, a[i]))
 			merged.push(a[i]);
   }
}
function mergeArraysRemovingDups(array1, array2) {
    var merged = new Array();
    mergePreservingOrder(merged, array1);
    mergePreservingOrder(merged, array2);
    return merged;
}

function buildRecipientList() {
   var recipientList = document.getElementById("recipient-list");

   deleteChildren(recipientList);   

   // add new ones
   var inputRecipients = document.getElementById("main:recipients");
   var recipients = fromCommaString(inputRecipients.value);
   
   if (recipients.length == 0) {
       var newNode = document.createElement("div");
       newNode.setAttribute("class", "no-recipients");
       newNode.appendChild(document.createTextNode("All alone!"));
       recipientList.appendChild(newNode);
   } else {
       for (var i = 0; i < recipients.length; ++i) {
          var newNode = document.createElement("div");
	      newNode.setAttribute("class", "recipient-list-item");
      	  newNode.appendChild(document.createTextNode(recipients[i]));
          recipientList.appendChild(newNode);
       }
   }
}

function validateAll() {
   var valid = true;
   var inputUrl = document.getElementById("main:url");
   var inputRecipients = document.getElementById("main:recipients");
   
   if (isJustWhitespace(inputUrl.value)) {
	  setError("url-error", "Give us a link to share!");
	  valid = false;
   } else {
      unsetError("url-error");
   }
   
   if (isJustWhitespace(inputRecipients.value)) {
      setError("recipients-error", "Who do you want to share this link with?");
      valid = false;
   } else {
      unsetError("recipients-error");
   }
   
   return valid;
}

// called when there's a change to the user-visible recipients input
function onRecipientsChanged() {
   var friendentry = document.getElementById("friendentry");
   var inputRecipients = document.getElementById("main:recipients");
   var newRecipients = fromCommaString(friendentry.value);
   var oldRecipients = fromCommaString(inputRecipients.value);
   friendentry.value = "";
  
   var mergedRecipients = mergeArraysRemovingDups(oldRecipients, newRecipients);
   
   inputRecipients.value = toCommaString(mergedRecipients);
   buildRecipientList();
   validateAll();
}

// ]]>
</script>
		
      </head>

<body onload="buildRecipientList()">

<c:if test="!${signin.valid}">
<script type="text/javascript" language="javascript">
// <![CDATA[
  loginRedirect(${login});
// ]]>
</script>
</c:if>

<div class="share-link">

<strong>Share Link</strong>

<h:form id="main" onsubmit="return validateAll();">

<!-- FIXME use Scriptaculous in-place edit widget, http://wiki.script.aculo.us/scriptaculous/show/Ajax.InPlaceEditor -->
<h:inputText id="url" styleClass="url" value="#{sharelink.url}" onkeypress="return onEnterFalse(event);" onchange="validateAll();"/>
<div id="url-error"></div>

<div class="recipients"><div class="label">Share <u>W</u>ith:</div>
<input autocomplete="off" accesskey="w" type="text" id="friendentry" class="autocomplete" onchange="onRecipientsChanged()" onkeypress="return onEnterFalse(event)"/>
<h:message for="recipients" styleClass="validity-error"/>
<div id="recipients-error"></div>
<div id="friendentry-choices" class="autocomplete-choices"></div>

<script type="text/javascript" language="javascript">
// <![CDATA[
new Ajax.Autocompleter("friendentry", "friendentry-choices", "${xmlfriendcompletions}", {method: "get", asynchronous: true,
                       paramName: "entryContents", tokens: ",",
                       afterUpdateElement: function(inputElement, selected) { onRecipientsChanged(); } } );
// ]]>
</script>
<br/>

<!-- change to inputText to debug -->
<h:inputHidden id="recipients" value="#{sharelink.recipients}" converter="#{sharelink.recipientsConverter}"/>

<div id="recipient-list" class="recipient-list">

</div>

</div><!-- class="recipients" -->

<div class="description"><div class="label"><u>D</u>escription:</div>
	<!-- FIXME mimick google talk ui and expand as they type more -->
	<h:inputTextarea id="description" accesskey="d" rows="2" styleClass="description" value="#{sharelink.description}"/>
</div>

<div class="share">
<h:commandButton accesskey="s" styleClass="share" value="Share" action="#{sharelink.doShareLink}"/></div>

</h:form>

</div><!-- class=share-link -->

</body>
</f:view>
</html>
