<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="welcomedisabled" class="com.dumbhippo.web.WelcomeDisabledPage" scope="request"/>

<c:if test="${!welcomedisabled.signin.disabled}">
	<!--  happens if you re-enable your account -->
	<jsp:forward page="/jsp/welcome.jsp"/>
</c:if>

<head>
	<title>Au Revoir!</title>
	<dht:stylesheets />
	<dht:scriptIncludes />
</head>
<body>
    <dht:header>
		Ciao!
	</dht:header>
	
	<div id="dhMain">
	
		<p>
		We've disabled your account and will not send 
		further invitation emails or shared items to <c:out value="${welcomedisabled.person.email}"/>.
		If you do receive more unwanted items at this address, please let us know 
		at <c:out value="${welcomedisabled.feedbackEmail}"/> and we will address the 
		issue as soon as we possibly can.
		</p>
	
		<p>
		You can <a href="javascript:dh.actions.setAccountDisabled(false);">re-enable your account</a>
		if you change your mind.
		</p>
		
		<p>
		Otherwise, <a href="javascript:dh.actions.signOut();">click here to sign out</a>.
		</p>
	</div>
</body>
</html>
