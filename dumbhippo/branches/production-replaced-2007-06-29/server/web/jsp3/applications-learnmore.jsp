<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Application Statistics</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
	<dh:script module="dh.actions"/>
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Open Source Application Statistics</div>
   		<div class="dh-applications-learnmore">
		    <p>
		    	Have you ever looked at a long list of software packages available for
		    	installation and wondered which ones people actually use? The Mugshot 
		    	and Fedora developers are working on new ways to find and browse 
		    	applications that take usage statistics into account. You can help out
		    	with the effort by sharing your application usage statistics.
	    	</p>
	    	<p>
				Right now, we're just providing <a href="/applications">global anonymous 
				statistics</a> about the applications that are most frequently used by
				Mugshot users. In the future, we may extend this with other features like
				seeing which applications are most frequently used among your circle of
				friends or finding local people who use a particular application.
	    	</p>
		    <h2>How it works</h2>
		    <p>
			    When application usage statistics are enabled for your account, the 
			    Mugshot software keeps track of which applications you interact with.
				Periodically it uploads a list of applications that you've used within the
				last day to the server. We don't record anything about what you're doing
				inside the applications, what documents you have open, or even how much
				time you spend inside each application.
			</p>
	    	<c:if test="${signin.valid}">
			    <h2>Your current setting</h2>
			    <div class="dh-applications-settings">
				    Application usage statistics: 
				    <c:choose>
						<c:when test="${signin.user.account.applicationUsageEnabledWithDefault}">
							<input type="radio" name="dhApplicationUsageEnabled" id="dhApplicationUsageOn" checked="true" onclick="dh.actions.setApplicationUsageEnabled(true);"> <label for="dhApplicationUsageOn">On</label>
							<input type="radio" name="dhApplicationUsageEnabled" id="dhApplicationUsageOff" onclick="dh.actions.setApplicationUsageEnabled(false);">	<label for="dhApplicationUsageOff">Off</label>			
						</c:when>
						<c:otherwise>
							<input type="radio" name="dhApplicationUsageEnabled" id="dhApplicationUsageOn" onclick="dh.actions.setApplicationUsageEnabled(true);"> <label for="dhApplicationUsageOn">On</label>
							<input type="radio" name="dhApplicationUsageEnabled" id="dhApplicationUsageOff" checked="true" onclick="dh.actions.setApplicationUsageEnabled(false);">	<label for="dhApplicationUsageOff">Off</label>
						</c:otherwise>
					</c:choose>
			    </div>
		    </c:if>
	    </div>
	</dht3:shinyBox>
</dht3:page>
