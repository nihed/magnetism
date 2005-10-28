<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<jsp:useBean id="signin" class="com.dumbhippo.web.SigninBean" scope="session"/>
<jsp:useBean id="viewperson" class="com.dumbhippo.web.ViewPersonPage" scope="request"/>
<jsp:setProperty name="viewperson" property="signin" value="${signin}"/>
<jsp:setProperty name="viewperson" property="viewedPersonId" param="personId"/>

<head>
	<title><c:out value="${viewperson.personInfo.humanReadableName}"/></title>
	<link rel="stylesheet" href="/css/person.css" type="text/css" />
	<script src="/javascript/config.js" type="text/javascript"></script>
    <script src="/javascript/dojo/dojo.js" type="text/javascript"></script>
    <script src="/javascript/common.js" type="text/javascript"></script>
    <script type="text/javascript">
	    dojo.require("dh.server");
	    
	    function addContact() {
	    	dh.server.doPOST("addcontactperson",
						     { "contactId" : "${viewperson.personInfo.person.id}" },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't add user to contact list");
				  	    	 });
	    }
	    function removeContact() {
	    	dh.server.doPOST("removecontactperson",
						     { "contactId" : "${viewperson.personInfo.person.id}" },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't add user to contact list");
				  	    	 });
	    }
    </script>
</head>
<body>
	<div class="header">
	<table>
		<tr>
		<td><span class="first-letter dh">D</span><span class="dh">umb</span><span class="first-letter dh">H</span><span class="dh">ippo</span></td>
		<td class="right"><c:out value="${viewperson.personInfo.humanReadableName}"/></td>
		</tr>
	</table>
	</div>
	<div class="toolbar">
	Do It: <a href="javascript:alert('DOJO');">&#187; Share</a> &#151; <a href="/jsf/home.faces">Your Page</a> &#151;
		<c:if test="${viewperson.isContact}"><a href="javascript:removeContact()">Remove <c:out value="${viewperson.personInfo.humanReadableName}"/> from my contact list</a></c:if>
		<c:if test="${!viewperson.isContact}"><a href="javascript:addContact()">I know <c:out value="${viewperson.personInfo.humanReadableName}"/></a></c:if>
	</div>

	<div class="main">

	<table>
	<tr><td>

	<div class="shared-links">	
		<strong>Cool Shared Links</strong>

		<c:forEach items="${viewperson.postInfos}" var="info">
		<div class="cool-bubble-shadow">		
		<table class="cool-bubble">
		<tr>
		    <td class="cool-person" rowSpan="3">
			<a class="cool-person" href="">
			<img class="cool-person" src="" />
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
	<strong>Groups:</strong>
	<br/>
	<dh:entityList value="${viewperson.groups}"/>
	</div>
	</td>
	</tr>
	</table>
	</div>

</body>
</html>
