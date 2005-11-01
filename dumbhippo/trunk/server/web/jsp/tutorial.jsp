<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="tutorial" class="com.dumbhippo.web.TutorialPage" scope="request"/>

<head>
	<title>How To Share a Link</title>
	<link rel="stylesheet" href="/css/tutorial.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header>
		Learn to share a link
	</dht:header>

	<dht:toolbar/>

	<p></p>
	
	<div class="dh-tutorial-movie-area">
		<div class="dh-tutorial-movie">Tutorial Movie Here</div>
	</div>

	<div class="main">
		<table class="dh-tutorial-edit-table">
		<tr>
		<td>
			Other people see you as:
		</td>
		<td>
			<dht:userNameEdit value="${tutorial.personInfo.humanReadableName}"/>
		</td>
		</tr>
		<tr>
		<td>
			Blah blah:
		</td>
		<td>
			<input/>
		</td>
		</tr>
		</table>
		
		<p>
		<a href="home">I get it! Return me to my page.</a>
		</p>
		
	</div>
	
	
</body>
</html>
