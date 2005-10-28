<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<jsp:useBean id="signin" class="com.dumbhippo.web.SigninBean" scope="session"/>
<jsp:useBean id="home" class="com.dumbhippo.web.HomePage" scope="request"/>
<jsp:setProperty name="home" property="signin" value="${signin}"/>

<head>
	<title><c:out value="${home.personInfo.humanReadableName}"/></title>
	<link rel="stylesheet" href="/css/home.css" type="text/css" />
	<script src="/javascript/config.js" type="text/javascript"></script>
    <script src="/javascript/dojo/dojo.js" type="text/javascript"></script>
    <script src="/javascript/common.js" type="text/javascript"></script>
    <script type="text/javascript">
	    dojo.require("dojo.widget.HtmlInlineEditBox");
	    dojo.require("dojo.event.*");
	    dojo.require("dh.server");
	    
	    var dhNameEntry;
	    
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
	    <c:url value="viewperson?personId=${home.signin.user.id}" var="publicurl"/>
		<a style="font-size:8pt"href="${publicurl}">(your public page)</a>
		</td>
		</tr>
	</table>
	</div>
	<div class="toolbar">
	<c:url value="sharelink?next=home" var="share"/>
	Do It: <a href="${share}">&#187; Share</a> &#151; <a href="/jsf/family.faces">Your Family Page</a>
	</div>

	<div class="person">
		<img class="cool-person" src="/files/headshots/${home.personInfo.person.id}" />
	   	<span dojoType="InlineEditBox" class="dhName" id="dhNameEntry">
	   	<c:out value="${home.personInfo.humanReadableName}"/>
	   	</span>
	</div>

	<div class="main">

	<table>
	<tr><td>

	<div class="shared-links">	
		<strong>Links Shared With You</strong>

		<c:forEach items="${home.receivedPostInfos}" var="info">
		<div class="cool-bubble-shadow">		
		<table class="cool-bubble">
		<tr>
		    <td class="cool-person" rowSpan="3">
			<a class="cool-person" href="">
			<img class="cool-person" src="/files/headshots/${info.posterInfo.person.id}" />
			<br/>
			<dh:entity value="${info.posterInfo}"/>
			</a>
		    </td>
		    <td class="cool-link">
			<div class="cool-link">
			<a class="cool-link" title="${info.url}" href="${info.url}"><c:out value="${info.title}"/></a>
			</div>
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
	
</html>
