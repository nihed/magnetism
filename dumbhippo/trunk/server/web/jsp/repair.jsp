<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.WelcomePage" scope="request"/>

<c:if test="${welcome.signin.disabled}">
	<jsp:forward page="/jsp/we-miss-you.jsp"/>
</c:if>

<head>
	<title>Repair DumbHippo</title>
	<dht:stylesheets href="welcome.css" iehref="bubbles-iefixes.css"/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.util");
		dojo.require("dh.welcome");
	</script>
</head>
<dht:body>
	<c:url value="person?who=${welcome.signin.userId}" var="publicurl"/>
	<dht:mainArea>
		<dht:toolbar/>
  
		<div class="dh-welcome-message">
			<p class="dh-welcome-headline">The DumbHippo Repair Page</p>
			
			<c:choose>
				
				<c:when test="${welcome.browser.windows}">
					<p>
						You are now logged into DumbHippo; the next step in repairing your
						DumbHippo configuration is to download and install the current
						version of the client software.
					</p>
					<p>
						<a href="${welcome.downloadUrlWindows}">Click here to download the DumbHippo software</a>.
					</p>
					<p>
						Once you've downloaded and installed the software, it will start running 
						automatically, and you'll be back up and running with all the features of
						DumbHippo.
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
						The full DumbHippo
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
	
	</dht:mainArea>
	<dht:bottom/>
</dht:body>
</html>
