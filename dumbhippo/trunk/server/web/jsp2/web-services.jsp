<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Web Services</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_webserv500.gif" fullHeader="true">
 
    <p>
    Mugshot uses public web services from third party providers, subject to the following 
    terms and conditions.
    </p>
    
    <a name="amazon">
    <h3>Amazon</h3>
    
    <p>Please keep in mind that some of the content that we make available to you through 
    this application comes from 
    <a href="http://www.amazon.com/gp/aws/landing.html">Amazon Web Services</a>. 
    All such content is provided to you "as is." This content and your use of it are 
    subject to change and/or removal at any time.</p>
    
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
