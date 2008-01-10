<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Web Services</title>
	<dht:siteStyle/>	
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_webserv500.gif" fullHeader="true">
 
    <p>
    Mugshot uses public web services from third party providers, subject to the following 
    terms and conditions.
    </p>
   
    <a name="aim">
    <h3>AIM</h3>
    
    <p>Mugshot is compatible with the AIM&reg; service and makes use of interfaces provided through the
    <a href="http://developer.aim.com">Open AIM</a> program.  All such services are provided to 
    you "as is." These services and your use of them are subject to change and/or removal at 
    any time.</p>
    
    <p>By providing your AIM Screen name, your online status may be publicly displayed.
    Visit the <a href="http://developer.aim.com/faq.jsp#presence">Open AIM FAQ</a> for
    more information about displays of online presence.</p>
    
    <a name="amazon">
    <h3>Amazon</h3>
    
    <p>Please keep in mind that some of the content that we make available to you through 
    this application comes from 
    <a href="http://www.amazon.com/gp/aws/landing.html">Amazon Web Services</a>. 
    All such content is provided to you "as is." This content and your use of it are 
    subject to change and/or removal at any time.</p>
    
    <a name="rhapsody">
    <h3>Rhapsody</h3>
    
    <p>Some of the content that we make available to you through this application 
    comes from <a href="http://www.rhapsody.com/webservices">Rhapsody web services</a>.  
    All such content is provided to you "as is." This content and your use of it 
    are subject to change and/or removal at any time.</p>
    
    <a name="yahoo">
    <h3>Yahoo!</h3>
    
    <p>Some of the content that we make available to you through this application 
    comes from
    <!-- Begin Yahoo Web Services HTML Attribution Snippet -->
    <a href="http://developer.yahoo.net/about/">
    Web Services by Yahoo!
    </a>
    <!-- End Yahoo Web Services HTML Attribution Snippet -->
    .  All such content is 
    provided to you "as is." This content and your use of it are subject to change 
    and/or removal at any time.
    </p>
    
</dht:systemPage>
</html>
