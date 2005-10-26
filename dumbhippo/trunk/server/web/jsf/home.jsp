<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>

<f:view>
	<head>
	<title><h:outputText value="#{home.personInfo.humanReadableName}"/></title>
	<link rel="stylesheet" href="/css/home.css" type="text/css" />
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

	<div class="header">
	<table>
		<tr>
		<td><span class="first-letter dh">D</span><span class="dh">umb</span><span class="first-letter dh">H</span><span class="dh">ippo</span></td>
		<td class="right">This is You!<br/>
	    <c:url value="viewperson.faces?personId=${home.signin.user.id}" var="publicurl"/>
		<a style="font-size:8pt"href="${publicurl}">(your public page)</a>
		</td>
		</tr>
	</table>
	</div>
	<div class="toolbar">
	Do It: <a href="javascript:alert('DOJO');">&#187; Share</a> &#151; <a href="javascript:alert('DOJO');">Your Famliy Page</a>
	</div>

	<div class="person">
	   	<span dojoType="InlineEditBox" class="dhName" id="dhNameEntry">
	   	<c:out value="${home.personInfo.humanReadableName}"/>
	   	</span>
	</div>

	<div class="main">

	<table>
	<tr><td>

	<div class="shared-links">	
		<strong>Links You Shared</strong>

		<c:forEach items="${home.receivedPostInfos}" var="info">
		<div class="cool-bubble-shadow">		
		<table class="cool-bubble">
		<tr>
		    <td class="cool-link">
			<a class="cool-link" title="${info.url}" href="${info.url}"><c:out value="${info.title}"/></a>
		    </td>
		</tr>
		<tr>
		    <td class="cool-link-desc">
			<c:out value="${info.post.text}"/>
		    </td>
		</tr>
		<tr>
		    <td class="cool-link-meta">
			<div class="cool-link-date">
				(<fmt:formatDate value="${info.post.postDate}" type="both"/>)
			</div>
			<div class="cool-link-to">
				<dh:entityList value="${info.recipients}"/>
			</div>
		   </td>
		</tr>
		</table>
		</div>

		</c:forEach>
	</div>

	</td>
	<td>
		<div class="groups">
		<strong>Groups You're In</strong>
		<br/>
		<dh:entityList value="${home.groups}"/>
		</div>

		<div class="friends">
		<strong>People You Know</strong>
		<br/>
		<dh:entityList value="${home.contacts}"/>
		</div>
	</td>
	</tr>
	</table>
	</div>

	</body>
	
</f:view>
</html>
