<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="sharelink" class="com.dumbhippo.web.pages.ShareLinkPage" scope="request"/>

<html>

<head>
   <title>Sharing a Link</title>
	<dht3:stylesheet name="sharelink-inactive"/>
	<dh:script modules="dh.util"/>
	<script>
		function dhOpenAndClose(url) {
			window.open(url, "_NEW");
			dh.util.closeWindow();
		}
	</script>
</head>

<body>
  <div>
    <img src="/images2/lslinkswarm.gif" />
    <c:choose>
    	<c:when test="${!signin.valid}">
		    <p>You need to be logged into Mugshot to share a link.</p>
		    <p>Please <a href="javascript:dhOpenAndClose('/who-are-you')">log in</a> and try again.</p>
    	</c:when>
    	<c:when test="${signin.user.account.adminDisabled}">
		    <p>
				Your account has been disabled by the Mugshot site adminstrators. If you believe it
				was disabled in error, please contact <a href="mailto:feedback@mugshot.org">feedback@mugshot.org</a> 
				so we can straighten the situation out.
		    </p>
    	</c:when>
    	<c:when test="${!signin.user.account.hasAcceptedTerms}">
		    <p>You haven't yet accepted the Mugshot Terms of use.</p>
		    <p>Please <a href="javascript:dhOpenAndClose('/account')">activate your account</a> and try again.</p>
    	</c:when>
    	<c:when test="${signin.user.account.disabled}">
		    <p>You have disabled your Mugshot account.</p>
		    <p>Please <a href="javascript:dhOpenAndClose('/account')">reenable your account</a> and try again.</p>
    	</c:when>
    	<c:otherwise>
    		<p>
    			Whoah! How did you get here?
   			</p>
    	</c:otherwise>
    </c:choose>
  </div>
</body>

</html>
