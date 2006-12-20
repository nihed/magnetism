<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:set var="pageName" value="Google Gadget" scope="page"/>

<head>
	<title>${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="google-stacker">
	<dht3:shinyBox color="grey">
		<div>
			&nbsp;
		</div>	
		<div>
			You can add the Mugshot Stacker Google Gadget to your Google personalized home  
			page to keep an eye on what your friends are doing online. Click the 
			<a href="http://fusion.google.com/ig/add?synd=open&moduleurl=${baseUrl}/google-stacker-spec"><img src="http://gmodules.com/ig/images/plus_google.gif"></a> button to 
			add this to your Google personalized home page. 
		</div>
		<div>
			&nbsp;
		</div>
		<div>
			<center>
				<script src="http://gmodules.com/ig/ifr?url=${baseUrl}&amp;synd=open&amp;w=320&amp;h=250&amp;title=Mugshot+Stacker&amp;border=%23ffffff%7C3px%2C1px+solid+%23999999&amp;output=js">
				</script>
			</center>
		</div>
		<div>
			&nbsp;
		</div>
		<div>
			Or, <a href="http://gmodules.com/ig/creator?synd=open&url=${baseUrl}/google-stacker-spec">get the code</a> to put this 
			gadget on your own web site (keep in mind, the Mugshot Stacker will show each person 
			viewing it what <em>they</em> are watching on Mugshot - it doesn't show your site 
			visitors your stacker, it shows them their stacker). To show off your own 
			Mugshot on your site, you might prefer <a href="/badges">these badges</a>.
		</div>
	</dht3:shinyBox>
</dht3:page>

</html>
