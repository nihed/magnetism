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
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar publicPageId="${home.signin.userId}" home="false"/>
		<dht:infobar/>
		<p>
		<dht:smallTitle>Post Publicity</dht:smallTitle>
		<p><dht:publicShareToggle defaultPublic="${home.signin.defaultSharePublic}"/></p>
		<dht:largeTitle>Stuff Shared With You</dht:largeTitle>

		<div id="dhSharesArea">
			<dht:postList posts="${home.receivedPosts.list}" maxPosts="${home.maxReceivedPostsShown}" recipientId="${home.person.user.id}" recipientName="${home.person.name}"/>
		</div>
	</dht:mainArea>

	<dht:sidebarArea>
	
		<dht:sidebarAreaHeader>
		    <dht:headshot person="${home.person}" size="192" />
		    <dht:uploadPhoto location="/headshots" linkText="change photo" reloadTo="/home"/>
		    <dht:sidebarAreaHeaderName value="${home.person.name}" canModify="true"/>
		</dht:sidebarAreaHeader>

		<dht:sidebarPanes>
		    <dht:sidebarPane title="Groups You're In">
			    <div class="dh-groups">
			        <c:choose>
				        <c:when test="${home.groups.size > 0}">
					        <dh:entityList value="${home.groups.list}" photos="true" bodyLengthLimit="8" twoLineBody="true"/>
				        </c:when>
				        <c:otherwise>
					        <!-- FIXME: need class definition for this -->
					        <div class="dh-groups-none">You Need Groups!!</div>
				        </c:otherwise>
			        </c:choose>
			    </div>
		    </dht:sidebarPane>
		    <dht:sidebarPane title="People You Know">
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
				    	    <dh:entityList value="${home.contacts.list}" showInviteLinks="${home.invitations > 0}" photos="true" bodyLengthLimit="8" longBodyLengthLimit="24" twoLineBody="true"/>
				    	    <p class="dh-right-box-text">
				    	        You have a total of ${home.totalContacts} friends on DumbHippo. You can view all your friends <a href="/friends">here</a>.
				    	    </p>    
				        </c:when>
				        <c:otherwise>
				  	        <!-- FIXME: need class definition for this -->
					        <div class="dh-people-none">You Need Peeps!!</div>
				        </c:otherwise>
			        </c:choose>
		    	</div>
		    </dht:sidebarPane>
	        <dht:sidebarPane last="true">
		  	    <p class="dh-right-box-text">
		  	    	<div><a href="/welcome">Download</a> the DumbHippo software for Windows</div>
		  	    	<div><a href="/bookmark">Create a bookmark</a> in Firefox or Safari</div>
		  	    	<div><a href="/nowplaying">Add a "Now Playing" music embed</a> to your blog</div>
			    </p>
		    </dht:sidebarPane>
		</dht:sidebarPanes>
	</dht:sidebarArea>

</dht:bodyWithAds>
</html>
