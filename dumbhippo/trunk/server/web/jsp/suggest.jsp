<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="suggest" class="com.dumbhippo.web.SuggestPage" scope="request"/>

<c:if test="${!suggest.signin.valid}">
	<!-- this is a bad error message but should never happen since we require signin to get here -->
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Suggestions</title>
	<dht:stylesheets href="home.css" iehref="home-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
	        dojo.require("dh.util");
	</script>
</head>
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<dht:toolbar publicPageId="${suggest.signin.userId}" home="false"/>

		<c:choose>
			<c:when test="${suggest.signin.disabled}">
				<% /* FIXME: Seems ridiculous to show this instead of just forward them to the account page */ %>
				<div id="dhInformationBar"><a class="dh-information" href="/account">(re-enable your account)</a></div>
			</c:when>
			<c:otherwise>
				<% /* FIXME: Leaving this option in case we want other messages */ %>
			</c:otherwise>
		</c:choose>

		<h2 class="dh-title">Suggestions</h2>

		<div id="dhSharesArea">
			<dht:postList posts="${suggest.recommendedPosts.list}" maxPosts="${suggest.maxRecommendedPostsShown}" recipientId="${suggest.person.user.id}" recipientName="${suggest.person.name}"/>
		</div>
		
	</div>

	<dht:bottom/>
</div>

<!-- check out this classy layout!!!  it's getting chilly in here! -->
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

		<h2>Your Ratings</h2>
		
		<table border=1 cellspacing=0 cellpadding=5>
		<tr>  
		  <th>rating id</th>
		  <th>type</th>
		  <th>post</th>
		  <th>score</th>
		  <th>reason</th>
		</tr>
		<c:forEach items="${suggest.ratings.list}" var="rating" varStatus="status">
		   <dht:rating rating="${rating}"/>
		</c:forEach>
		</table>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>
