<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%-- We have two versions of this page - the first is used when we are in a dialog
     displayed by the client, the second when we are triggered by the user using
     the web page --%>
<c:choose>
	<c:when test='${param["next"] == "close"}'>
		<head>
			<title>Sign-in Link Sent</title>
			<dht:stylesheets href="small-box.css" />
		</head>
		<dht:bodySmallBox>
			<dht:smallBoxTopArea>
				<h3>
					Sign-in link sent to <c:out value="${address}"/>; click on that link to sign in.
				</h3>
				<p style="text-align: right">
					<input type="button" value="&nbsp;&nbsp;OK&nbsp;&nbsp;" onclick="window.close();"/>
				</p>
			</dht:smallBoxTopArea>
		</dht:bodySmallBox>
	</c:when>
	<c:otherwise>
		<head>
			<title>Sign-in Link Sent</title>
			<dht:stylesheets />
		</head>	
		<dht:bodyWithAds>
			<dht:mainArea>
				<dht:toolbar/>
		
				<h3>
					Sign-in link sent to <c:out value="${address}"/>; click on that link to sign in.
				</h3>
				
				<p><a href="/home">Home</a></p>
				<p><a href="/main">Main</a></p>
			</dht:mainArea>
		</dht:bodyWithAds>
	</c:otherwise>
</c:choose>
</html>
