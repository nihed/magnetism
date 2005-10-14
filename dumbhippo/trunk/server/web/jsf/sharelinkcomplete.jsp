<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>

<f:view>
  <c:url value="/css/sitewide.css" var="pagestyle"/>
  
	  <head>
 	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  		<title>Link Shared!</title>

		<link rel="stylesheet" href="${pagestyle}" type="text/css"></style>
	
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="hippoembed"></object>
	<script type="text/javascript" language="javascript">
// <![CDATA[

function closeWindow() {
    var hippoembed = document.getElementById("hippoembed");
	if (hippoembed && hippoembed.readyState && hippoembed.readyState >= 3)
		hippoembed.CloseWindow();
}
	
// ]]>
</script>
      </head>

<body onload="closeWindow();">

<p>And it's off! The link is en route to your recipients.</p>

<p><h:commandLink action="sharelink">
	<h:outputText value="Share another link" />
</h:commandLink></p>

<p><h:commandLink action="main">
	<h:outputText value="Main page" />
</h:commandLink></p>

</body>
</f:view>
</html>
