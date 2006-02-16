<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="home" class="com.dumbhippo.web.HomePage" scope="request"/>

<c:if test="${!home.signin.valid}">
	<!-- this is a bad error message but should never happen since we require signin to get here -->
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Your Home</title>
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

		<dht:toolbar publicPageId="${home.signin.userId}" home="false"/>

		<c:choose>
			<c:when test="${home.signin.disabled}">
				<% /* FIXME: Seems ridiculous to show this instead of just forward them to the account page */ %>
				<div id="dhInformationBar"><a class="dh-information" href="/account">(re-enable your account)</a></div>
			</c:when>
			<c:otherwise>
				<% /* FIXME: Leaving this option in case we want other messages */ %>
			</c:otherwise>
		</c:choose>

		<h2 class="dh-title">Stuff Shared With You</h2>

		<div id="dhSharesArea">
			<dht:postList posts="${home.receivedPosts.list}" maxPosts="${home.maxReceivedPostsShown}" recipientId="${home.person.user.id}" recipientName="${home.person.name}"/>
		</div>
	</div>

	<div id="dhPersonalArea">
	
		<div id="dhPhotoNameArea">
		    <dht:headshot person="${home.person}" size="192" />
		    <dht:uploadPhoto location="/headshots" linkText="change photo" reloadTo="/home"/>
		    <div id="dhName">
		        <dht:userNameEdit value="${home.person.name}"/>
		    </div>
		</div>

		<div class="dh-right-box-area">		
		    <div class="dh-right-box">
			    <h5 class="dh-title">Groups You're In</h5>
			    <div class="dh-groups">
			        <c:choose>
				        <c:when test="${home.groups.size > 0}">
					        <dh:entityList value="${home.groups.list}" photos="true" bodyLengthLimit="12"/>
				        </c:when>
				        <c:otherwise>
					        <!-- FIXME: need class definition for this -->
					        <div class="dh-groups-none">You Need Groups!!</div>
				        </c:otherwise>
			        </c:choose>
			    </div>
		    </div>		
		    <div class="dh-right-box dh-right-box-last">
			    <h5 class="dh-title">People You Know</h5>
		  	    <p class="dh-right-box-text">
                    <c:choose>           
			            <c:when test="${home.invitations > 0}">
			                You can <a class="dh-invites-left" href="/invite">invite</a> ${home.invitations} more people to join DumbHippo.
			            </c:when>
			            <c:otherwise>
			                You don't have invitations to send out available to you at the moment.
			            </c:otherwise>
			        </c:choose>    
                    <br/>
                    You can manage your invites <a class="manage-invites" href="/invites">here</a>.
			    </p>
			    <div class="dh-people">
			        <c:choose>
			   	        <c:when test="${home.contacts.size > 0}">
				    	    <dh:entityList value="${home.contacts.list}" showInviteLinks="${home.invitations > 0}" photos="true" bodyLengthLimit="12"/>
				        </c:when>
				        <c:otherwise>
				  	        <!-- FIXME: need class definition for this -->
					        <div class="dh-people-none">You Need Peeps!!</div>
				        </c:otherwise>
			        </c:choose>
		    	</div>
		    </div>		    		    
		</div>
		
	</div>
	<dht:bottom/>
</div>

<div id="dhOTP">
    <dht:rightColumn/>
</div>

</body>
</html>
