<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.BadgesPage" pickRandomIfAnonymous="true"/>

<c:set var="pageName" value="Badges" scope="page"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="badges"/>
	<dht:faviconIncludes/>
	<dh:script module="dh.util"/>
	<script type="text/javascript">
		var dhBadgesAll = [ <c:forEach var="badge" items="${person.badges.list}" varStatus="status"><dh:jsString value="${badge.name}"/> ${status.last ? '];' : ','}</c:forEach>

		var dhShowBadge = function(name) {
			for (var i = 0; i < dhBadgesAll.length; ++i) {
				var b = dhBadgesAll[i];
				var previewNode = document.getElementById('dhBadgePreview' + b);
				var codeNode = document.getElementById('dhBadgeCode' + b);
				
				if (!previewNode)
					throw new Error("missing dhBadgePreview" + b);

				// codeNode is null if we are anonymous/logged-out
				
				if (b == name) {
					dh.log("  showing preview " + i + " " + previewNode.id);
					previewNode.style.display = 'block';
					if (codeNode)
						codeNode.style.display = 'block';
				} else {
					dh.log("  hiding preview " + i + " " + previewNode.id);
					previewNode.style.display = 'none';
					if (codeNode)
						codeNode.style.display = 'none';
				}
			}
		}
		var dhUpdateVisibleBadge = function() {
			for (var i = 0; i < dhBadgesAll.length; ++i) {
				var b = dhBadgesAll[i];
				var checkbox = document.getElementById('dhBadgeCheckbox' + b);
				if (!checkbox)
					throw new Error("missing dhBadgeCheckbox" + b);
				if (checkbox.checked) {
					dh.log("Checkbox is checked for " + i + " " + checkbox.id);
					dhShowBadge(b);
				} else {
					dh.log("Checkbox is not checked for " + i + " " + checkbox.id);
				}
			}
		}
		
		var dhOnClickBadgeCheckbox = function(event) {
			dhUpdateVisibleBadge();
		
			// keep bubbling
			return true;
		}
	</script>
</head>

<dht3:page currentPageLink="badges">
	<dht3:shinyBox color="grey">
	
		<div class="dh-badges-centered-column-container">
			<div class="dh-badges-centered-column">
				<div class="dh-badges-top">
					<div class="dh-badges-top-logo" style="width: 190px; height: 90px;">
						<dh:png src="/images3/${buildStamp}/minimugshot_logo.png" style="width: 190px; height: 90px;"/>
					</div>
					<div class="dh-badges-top-text" style="position: relative; width: 490px; height: 90px;">
						<div style="position: absolute; right: 0px; bottom: 0px; width: 490px;">
							<div class="dh-badges-top-text-headline">
								Put Mugshot on your site with our free widget.
							</div>
							<div class="dh-badges-top-text-body">
								Got lots of web accounts? Show where you're at and what you're up to at a glance. 
								Add Mini Mugshot to blogs, MySpace, and more.
							</div>
							<c:if test="${!signin.valid}">
								<div class="dh-badges-signup-or-login">
								    <table cellspacing="0" cellpadding="0">
								    	<tbody>
										    <tr>
											    <td><a href="/signup"><img src="/images3/${buildStamp}/signup.gif"/></a></td>
											    <td valign="middle" align="center" class="dh-badges-signup-or-login-text">&nbsp;or&nbsp;</td>
											    <td><a href="/who-are-you?next=badges"><img src="/images3/${buildStamp}/login.gif"/></a></td>
											    <td valign="middle" align="center" class="dh-badges-signup-or-login-text">&nbsp;to get your own!</td>
										    </tr>
										</tbody>
								    </table>
								</div>
							</c:if>
						</div>
					</div>
					<div class="dh-grow-div-around-floats"><div></div></div>
				</div>
			
				<div class="dh-badges-step-box">
					<div class="dh-badges-step-box-left">
						<div class="dh-badges-step-title">
							<c:choose>
								<c:when test="${person.self}">
									<span class="dh-badges-step-number">Step 1:</span> Choose a style
								</c:when>
								<c:otherwise>
									See a live example
								</c:otherwise>
							</c:choose>
						</div>
						<div>
							<c:forEach var="badge" items="${person.badges.list}" varStatus="status">
								<div id="dhBadgeOption${badge.name}">
									<input type="radio" name="badge" value="${badge.name}" id="dhBadgeCheckbox${badge.name}" ${status.first ? 'checked' : ''} onclick="return dhOnClickBadgeCheckbox();"> <label for="dhBadgeCheckbox${badge.name}"><c:out value="${badge.optionName}"/></label>
								</div>
							</c:forEach>
						</div>
					</div>
					<div class="dh-badges-step-box-right" style="height: ${person.maxBadgeHeight}px;">
						<c:forEach var="badge" items="${person.badges.list}" varStatus="status">
							<div id="dhBadgePreview${badge.name}" style="display: ${status.first ? 'block' : 'none'}; width: ${badge.width}px; height: ${badge.height}px;">
								<dh:flashBadge badge="${badge}" userId="${person.viewedPerson.user.id}" hasLabel="false"/>
								<c:if test="${dh:enumIs(badge, 'NOW_PLAYING_440_120')}">
									<div style="margin-top: 5px;">
										<a style="font-size: 12px;" class="dh-underlined-link" href="/radar-learnmore">Browse, edit, and create Music Radar themes</a>
									</div>
								</c:if>
							</div>
						</c:forEach>
					</div>
					<div class="dh-grow-div-around-floats"><div></div></div>
				</div>
				<div class="dh-grow-div-around-floats"><div></div></div>
				
				<c:if test="${person.self}">
					<div class="dh-badges-step-box">
						<div class="dh-badges-step-box-left">
							<div class="dh-badges-step-title">
								<span class="dh-badges-step-number">Step 2:</span> Get the code
							</div>
							<div>
								Copy all the code and paste it into your site.
							</div>
						</div>
						<div class="dh-badges-step-box-right">
							<c:forEach var="badge" items="${person.badges.list}" varStatus="status">
								<div id="dhBadgeCode${badge.name}" style="display: ${status.first ? 'block' : 'none'};">
									<!--  don't add whitespace inside the textarea tag -->
									<textarea class="dh-badges-code" readonly="readonly" rows="8" cols="50" wrap="off"><dh:flashBadge badge="${badge}" escapeXml="true" embedOnly="true" userId="${person.viewedPerson.user.id}"/></textarea>
								</div>
							</c:forEach>
						</div>
						<div class="dh-grow-div-around-floats"><div></div></div>
					</div>
					<div class="dh-grow-div-around-floats"><div></div></div>
				</c:if>
			</div>
		</div>
	</dht3:shinyBox>
</dht3:page>

<script type="text/javascript">
	// this is needed since the browser may remember the previously-checked 
	// checkbox and thus differ from our default.
	dhUpdateVisibleBadge();
</script>

</html>
