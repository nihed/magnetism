<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="account" class="com.dumbhippo.web.AccountPage" scope="request"/>

<c:if test="${!account.signin.valid}">
	<!-- this is a bad error message but should never happen since we require signin to get here -->
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Your Account Setup</title>
	<dht:stylesheets href="account.css" iehref="account-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.password");
		dojo.require("dh.slideshow");
        dojo.require("dh.util");
	</script>
	<script type="text/javascript">	
		var accountSlideshowInit = function() {
		
			var slideNode = document.createElement("div");
			slideNode.setAttribute("id", "dhSlideshowTextarea");
			var p = document.createElement("p");
			p.appendChild(document.createTextNode("Then share that link with yourself, go on do it!!"));
			p.setAttribute("class", "dh-slideshow-text");
			var link = document.createElement("a");
			link.setAttribute("href", "/home");
			link.setAttribute("target", "_new");
			link.setAttribute("class", "dh-slideshow-link");
			link.appendChild(document.createTextNode("Open This Link To Your Home Page"));
			slideNode.appendChild(link);
			slideNode.appendChild(p);
			
			var slides = [
				{ 	"time" : 9500, 
					"src" : "/images/tpfd-slides/001.jpg" },
				{ 	"time" : 12000,
					"src" : "/images/tpfd-slides/002.jpg" },
				{ 	"time" : 11000,
					"src" : "/images/tpfd-slides/003.jpg" },
				{ 	"time" : 11000,
					"src" : "/images/tpfd-slides/004.jpg" },
				{ 	"time" : 12000,
					"src" : "/images/tpfd-slides/005.jpg" },
				{ 	"time" : 11000,
					"src" : "/images/tpfd-slides/006.jpg" },
				{ 	"time" : 6500,
					"src" : "/images/tpfd-slides/007.jpg" },
				{ 	"time" : 0,
					"node" : slideNode }
			];
		
			var node = document.getElementById('dhAccountSlideshow');
			var slideshow = new dh.slideshow.Slideshow(node, 420, 300, slides);
		}
		
		dojo.event.connect(dojo, "loaded", dj_global, "accountSlideshowInit");	
	</script>
</head>
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<dht:toolbar account="false" publicPageId="${account.signin.userId}"/>

		<h2 class="dh-title">Comics!</h2>

		<div style="overflow:hidden;">

			<div class="dh-account-movie-area">
				<div id="dhAccountSlideshow"></div>
			</div>
			<div style="clear:left;text-align:center;"><a style="font-size:xx-small;font-style:italic;color:black;text-decoration:none;" target="_blank" href="http://www.toothpastefordinner.com/">temporary comics based on the awesome toothpastefordinner characters</a></div>

		<c:choose>
			<c:when test="${!account.signin.disabled}">
				
				<div class="dhBackgroundBox">
					<b>Help your friends find you</b>
			
					<table class="dh-edit-table">
					<tr>
					<td class="dh-edit-table-label">
						Your name is:
					</td>
					<td class="dh-edit-table-control">
						<dht:userNameEdit value="${account.person.name}"/>
					</td>
					</tr>

					<tr>
					<td colspan="2" class="dh-explanation">Click on the name to change it.
					</td>
					</tr>
					</table>
				</div>
		
				<br/>
		
				<div class="dhBackgroundBox">
					<b>Security and privacy</b>
					
					<table class="dh-edit-table">
					<tr>
					<td class="dh-edit-table-label">
						Set a password:
					</td>
					<td class="dh-edit-table-control">
						<input type="password" class="dhText" id="dhPasswordEntry"/>
					</td>
					</tr>
					<tr>
					<td class="dh-edit-table-label">
						Type the same password again:
					</td>
					<td class="dh-edit-table-control">
						<input type="password" class="dhText" id="dhPasswordAgainEntry"/>
					</td>
					</tr>
					<tr>
					<td class="dh-edit-table-label">
					</td>
					<td class="dh-edit-table-control">
						<input type="button" value="Set Password" id="dhSetPasswordButton"/>
					</td>
					</tr>			
					<tr>
					<td colspan="2" class="dh-explanation">You can email or IM yourself a sign-in link at any time, so a password is optional.
					</td>
					</tr>
					</table>
				</div>
			</c:when>
			<c:otherwise>
				<div>				
					<a href="javascript:dh.actions.setAccountDisabled(false);">Enable Account</a>

					<p>Enabling your account will give you a public page and let you share links and photos with other people.  <a href="/privacy" target="_blank">Our privacy policy</a></p>
				</div>
				<div>
					<a href="javascript:dh.actions.signOut();">Sign Out</a>
				</div>
			</c:otherwise>
		</c:choose>		

		</div>
	</div>

	<div id="dhPersonalArea">
		<div id="dhPhotoNameArea">
		<dht:headshot person="${account.person}" size="192" />
		<dht:uploadPhoto location="/headshots" linkText="change photo"/>
		<div id="dhName"><c:out value="${account.person.name}"/></div>
		</div>

		<div class="dh-right-box-area">
		<c:if test="${!account.signin.disabled}">
			<div class="dh-right-box">
				<h5 class="dh-title">Email Addresses</h5>
					<c:forEach items="${account.person.allEmails}" var="email">
						<p><c:out value="${email.humanReadableString}"/></p>
					</c:forEach>	
					<p><input id="dhExtraEmailAddress" class="dhText"/><input type="button" value="Add Email Address"/></p>
					<p class="dh-explanation">Click on the link you get in the mail.</p>
			</div>
			<div class="dh-right-box">
				<h5 class="dh-title">Screen Names</h5>
					<c:forEach items="${account.person.allAims}" var="aim">
						<p><c:out value="${aim.humanReadableString}"/></p>
					</c:forEach>
					<p><a href="${account.addAimLink}">AIM us your screen name</a></p>
					<p class="dh-explanation">You can use it to login, we'll keep it private (just between us).</p>
			</div>
		</c:if>
			<div class="dh-right-box">
				<h5 class="dh-title">Account Options</h5>
			<c:choose>
				<c:when test="${!account.signin.disabled}">
				<div>
				<a href="javascript:dh.actions.signOut();">Sign Out</a>
				<p>Sign out to keep other people on this computer from using your account.</p>
				</div>
				<div>
				<a href="javascript:dh.actions.setAccountDisabled(true);">Disable Account</a>
				<p>Disabling your account means you have no public page and we will never send you email for any reason.	<a href="/privacy" target="_blank">Our privacy policy</a></p>
				</div>
				</c:when>
				<c:otherwise>
				<div>
					<a href="javascript:dh.actions.setAccountDisabled(false);">Enable Account</a>
					<p>Enabling your account will give you a public page and let you share links and photos with other people.  <a href="/privacy" target="_blank">Our privacy policy</a></p>
				</div>
				<div>
					<a href="javascript:dh.actions.signOut();">Sign Out</a>
				</div>
				</c:otherwise>
			</c:choose>		
			</div>
		</div>
	</div>

</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>
