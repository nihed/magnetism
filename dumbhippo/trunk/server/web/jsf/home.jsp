<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>

<f:view>
	<head>
	<title><h:outputText value="#{home.personInfo.humanReadableName}"/></title>
	<script src="/javascript/config.js" type="text/javascript"></script>
    <script src="/javascript/dojo/dojo.js" type="text/javascript"></script>
    <script src="/javascript/common.js" type="text/javascript"></script>
    <script type="text/javascript">
	    dojo.require("dojo.widget.HtmlInlineEditBox");
	    dojo.require("dojo.event.*");
	    dojo.require("dh.server");
	    
	    function dhNameEntryOnSave(value, oldValue) {
	    	dh.server.doPOST("renameperson",
						     { "name" : value },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't rename user");
				  	    	     dhNameEntry.setText(oldValue);
				  	    	 });
	    }
	    
		function init() {
			dhNameEntry = dojo.widget.manager.getWidgetById("dhNameEntry");
			dojo.event.connect(dhNameEntry, "onSave", dj_global, "dhNameEntryOnSave");
	    }

	    dojo.event.connect(dojo, "loaded", dj_global, "init");
    </script>
	</head>
	<body>
	<div class="person">
		<p>
	   	<span dojoType="InlineEditBox" class="dhName" id="dhNameEntry">
	   	<c:out value="${home.personInfo.humanReadableName}"/>
	   	</span>
	    <c:url value="viewperson.faces?personId=${home.signin.user.id}" var="publicurl"/>
		(<a href="${publicurl}">public page</a>)</p>
		<div class="shared-links">
		<p>Recently seen links:</p>	
		<table>
		<c:forEach items="${home.receivedPostInfos}" var="info">
		    <tr>
			    <td colspan="2"><strong><a href="${info.url}"><c:out value="${info.title}"/></a></strong></td>
			</tr>
			<tr>
				<th align="right">From:</th>
				<td>
				<dh:entity value="${info.posterInfo}"/>
				(<fmt:formatDate value="${info.post.postDate}" type="both"/>)
				</td>
			</tr>
			<tr>
			    <th align="right">To:</th><td><dh:entityList value="${info.recipients}"/></td>
		    </tr>
		    <tr>
		    	<th></th><td><c:out value="${info.post.text}"/></td>
		    </tr>
		</c:forEach>
		</table>
		</div>
		<br/>
		<div class="groups">
		<p><strong>Groups:</strong> <dh:entityList value="${home.groups}"/></p>
		<!--  ability to join/share groups [D] -->
		</div>
	</div>	
	</body>
	
</f:view>
</html>
