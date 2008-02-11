<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<%@ attribute name="currentPageLink" required="true" type="java.lang.String" %>

<c:choose>
	<c:when test="${currentPageLink == 'who-are-you'}">
		<c:set var="disableToggleLoginLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'signup'}">
		<c:set var="disableSignupLink" value="true"/>
	</c:when>	
</c:choose>

<div id="gnomeHeader">
	<div id="gnomeHeaderLeft" style="float: left;">
		<c:choose>
			<c:when test="${dogfoodMode}">
				<a class="gnome-header" href="">GNOME Online <span style="color: red;">DOGFOOD</span></a>
			</c:when>
			<c:otherwise>
				<a class="gnome-header" href="">GNOME Online</a>
			</c:otherwise>
		</c:choose>
	</div>		
	<div id="gnomeHeaderRight" style="float: right;">
		<c:choose>
			<c:when test="${disableToggleLoginLink}">				
			</c:when>
			<c:when test="${signin.valid}">
				<span>Hello, <c:out value="${signin.viewedUserFromSystem.name}"/>!</span> 
				<div><a class="dh-underlined-link" href="javascript:dh.actions.signOut();" title="Keep others from using your account on this computer">Log out</a></div>
			</c:when>
			<c:otherwise>	
			    <span><a class="dh-underlined-link" href="/who-are-you">Log in</a><c:if test="${!disableSignupLink}"> | <a class="dh-underlined-link" href="/who-are-you">Sign up</a></c:if></span>
			</c:otherwise>
		</c:choose>
	</div>
	<%-- grow around float --%>
	<div style="clear: both; height: 1px;"><div></div></div>
</div>
<gnome:fixedLogo/>
