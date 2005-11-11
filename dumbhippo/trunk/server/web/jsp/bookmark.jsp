<html>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="signin" class="com.dumbhippo.web.SigninBean" scope="request"/>

<head>
        <title>Bookmark This Link</title>
        <link rel="stylesheet" href="/css/group.css" type="text/css" />
        <dht:scriptIncludes/>
</head>
<body>
        <dht:header>
                Bookmark
        </dht:header>
        <dht:toolbar/>


<h2>Bookmark this link for use in Firefox or IE.</h2>

<%
String hostname = request.getServerName();
int _port = request.getServerPort();
String port = (_port == 80)? "" : ":" + _port;
%>

<p>
&nbsp;
</p>

<p><a href="javascript:window.open('http://<%= hostname + port %>/sharelink?v=1&url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title)+'&next=close','_NEW','menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=450,width=550,top='+((screen.availHeight-450)/2)+',left='+((screen.availWidth-550)/2));void(0);">Dumb Hippo It!</a></p>
