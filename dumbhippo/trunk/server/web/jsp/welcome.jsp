<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.WelcomePage" scope="request"/>

<c:if test="${welcome.signin.disabled}">
	<jsp:forward page="/jsp/we-miss-you.jsp"/>
</c:if>

<head>
	<title>Welcome <c:out value="${welcome.person.name}"/>!</title>
	<dht:stylesheets href="welcome.css" iehref="bubbles-iefixes.css"/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
		dojo.require("dh.welcome");
	</script>
</head>
<body>
<c:url value="person?who=${welcome.signin.userId}" var="publicurl"/>
<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<dht:toolbar/>
  
		<div class="dh-welcome-message">
			<p class="dh-welcome-headline">Welcome to DumbHippo</p>
			
			<c:choose>
				<c:when test="${welcome.browser.windows && welcome.fromMySpace}">
					<%-- For now, we don't provide a no-Windows + MySpace Music option,
					     since we consider our live Music features the most interesting part
					     for the MySpace crowd --%>
					<p>
						DumbHippo is a new way to share things with your friends. Letting
						your friends see what music you are playing currently is one
						of our features, but there's lots more too: for example, you can send
						interesting web pages to your friends and chat about them live. 
						If you tell us our MySpace name, you'll  even get bubble notifications 
						when your buddies add comments on your MySpace page!
					</p>
					<input type="radio" name="dhMySpaceRadio" id="dhMySpaceRadioNow" value="now" checked="yes" onclick="dh.welcome.onNowSelected()"></input> 
					My MySpace name is: <input id="dhMySpaceName" class="dhText" onkeyup="dh.welcome.updateDownload()" onchange="dh.welcome.updateDownload()" value="${welcome.person.account.mySpaceName}"></input>
					<br/>
					<input type="radio" name="dhMySpaceRadio" id="dhMySpaceRadioLater" value="later" onclick="dh.welcome.onLaterSelected()">I'll tell you my MySpace name later.</input>
					<p>
						To take advantage of the features of DumbHippo, you'll want to
						download and install our client software.
					</p>
					<input type="button" value="Download Now" id="dhMySpaceDownload" onclick="dh.welcome.doDownload('${welcome.downloadUrlWindows}')"></input>
				</c:when>
				
				<c:when test="${welcome.browser.windows}">
					<div class="dh-big-download-box">
						<div class="dh-center-children">
							<a href="${welcome.downloadUrlWindows}">Download Now</a>
						</div>
						DumbHippo software is free.
					</div>
			
					<p>
						DumbHippo is a new way to share things with your friends. You can
						see what people have shared with you on this web page, but to get the
						full experience, you'll want to download and install our software.
						Once you've installed the DumbHippo software, you'll get notification 
						when someone shares a link or message with you, and you'll be able
						to share the web pages you are browsing with other people.
					</p>
					<p>
						Click <a href="${welcome.downloadUrlWindows}">Download Now</a> to try it out!
					</p>
					
					<c:if test="${!welcome.browser.ie}">
						<p>
							It looks like you're using a browser other than Internet Explorer.
							In addition to downloading the application, you'll want to
							<a href="/bookmark">go here</a> for a bookmark to add to your browser
							toolbar.
						</p>
					</c:if>
					
				</c:when>
				<c:otherwise>
				
					<p>
						DumbHippo is a new way to share things with your friends. The full 
						experience relies on a <a href="${welcome.downloadUrlWindows}">Windows-only download</a>
						(we're working on support for other platforms!). However, you can use the basics
						without downloading the application. <a href="/bookmark">Go here</a> to learn how
						to add a handy bookmark to your browser.
					</p>
					
				</c:otherwise>
			</c:choose>
			
			<c:if test="${!welcome.browser.supported}">
				<p>
					We usually test the web site with Internet Explorer 5.5, Internet Explorer 6, Firefox 1.0 and Firefox 1.5. 
					We don't know if the site will work with your browser.
					Please give it a try and if you have trouble let us know at 
					<a href="mailto:${welcome.feedbackEmail}"><c:out value="${welcome.feedbackEmail}"/></a>.
				</p>
			</c:if>
		</div>
	
		<table>
		<tr>
		<c:if test="${welcome.receivedPosts.size > 0}">
			<td>
				<div class="shared-links">	
					<strong>Cool Shared Links</strong>
					<dht:postList posts="${welcome.receivedPosts.list}" maxPosts="${welcome.maxReceivedPostsShown}" recipientId="${welcome.person.user.id}" recipientName="${welcome.person.name}"/>
				</div>
			</td>
		</c:if>
		<td>
			<c:if test="${welcome.groups.size > 0}">
				<div class="groups">
					<strong>Groups You're In</strong><br/>
					<dh:entityList value="${welcome.groups.list}" photos="true"/>
				</div>
			</c:if>
			<div class="friends">
				<strong>People who invited you</strong><br/>
				<dh:entityList value="${welcome.inviters}" photos="true"/>
			</div>
		</td>
		</tr>
		</table>
	</div>
	<dht:bottom/>
</div>
</body>
</html>
