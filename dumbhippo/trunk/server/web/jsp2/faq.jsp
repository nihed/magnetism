<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>FAQ</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/${buildStamp}/header_faq500.gif" fullHeader="true">
 
    For general press inquiries, please contact 
    <a href="http://www.redhat.com/about/news/presskit/">Red Hat Media Relations</a>.
 
    <p>
    This Frequently Asked Question list is intended for journalists and analysts.
    Potential open source contributors should use the 
    <a href="http://developer.mugshot.org">Mugshot Developer Site</a> as a jumping off point.
    </p>
    
    
<h3>Table of contents</h3>

<ol>
<li><a href="#1">What is Mugshot?</a></li>
<li><a href="#2">What does it do?</a></li>
<li><a href="#3">Why should I care?</a></li>
<li><a href="#4">What does "open project" mean?</a></li>
<li><a href="#5">What's a "live social experience"?</a></li>
<li><a href="#6">What is available as of May 31, 2006?</a></li>
<li><a href="#7">What's next?</a></li>
<li><a href="#8">How do I get involved?</a></li>
<li><a href="#9">What is the relationship to existing social networking sites?</a></li>
<li><a href="#10">What is the relationship to existing music services and applications?</a></li>
<li><a href="#11">What is the relationship to instant messaging services?</a></li>
<li><a href="#12">What is the relationship to online services initiatives?</a></li>
<li><a href="#13">Is the music sharing feature legal?</a></li>
<li><a href="#14">How does Mugshot relate to Red Hat?</a></li>
<li><a href="#15">How does Mugshot benefit Red Hat?</a></li>
<li><a href="#16">What platforms and services are supported?</a></li>
<li><a href="#17">What web services does Mugshot use?</a></li>
</ol>

<a name="1">
<h3>1. What is Mugshot?</h3>

<p>
Mugshot is an open project to create a live social experience around
entertainment.
</p>

<a name="2">
<h3>2. What does it do?</h3>
   
<p>
Mugshot currently offers two activities:
</p>

<ul>
<li>Web Swarm - Share web links with individuals or groups in real time, and get live feedback when people visit those links</li>
<li>Music Radar - Show off the music you listen to using services like iTunes, Yahoo! Music, and others on your web site, blog or MySpace page</li>
</ul>

<p>
Mugshot works with mainstream applications like iTunes, Yahoo! Music Engine,
Firefox and Internet Explorer and currently supports Windows XP and
Linux platforms, with limited support for Apple's OS X.
</p>

<a name="3">
<h3>3. Why should I care?</h3>
   
<ul>
<li>Mugshot adds new live social experiences to existing applications and provides a platform for building new ones</li>
<li>Mugshot contributes to a leveling of the playing field between commercial and non-commercial content and service providers</li>
<li>Mugshot is designed for mainstream users, not just techies or IT departments</li>
<li>Mugshot is 100% open source, including the server and clients </li>
</ul>

<a name="4">
<h3>4. What does "open project" mean?</h3>
   
<p>
The software that powers Mugshot is 100% open source, including the
client and the server.
</p>

<p>
It's designed and developed in the context of an open source community
project at <a href="http://developer.mugshot.org">http://developer.mugshot.org</a>.
</p>


<a name="5">
<h3>5. What's a "live social experience"?</h3>
   
<p>
Computers today are built around the "desktop" metaphor; documents,
filing cabinets, that sort of thing. But today we use computers more for
communicating with other people than for processing documents in
isolation. Applications such as IM, MySpace, email, Flickr, and so
forth show that people are much more interesting than virtual filing
cabinets.
</p>

<p>
With Mugshot, we want to create live social experiences through design
centered on people and their activities, not documents and files.
</p>

<a name="6">
<h3>6. What is available as of May 31, 2006?</h3>
   
<ul>
<li>The Mugshot open source project site, including source code for the client and server, project information, blog, and more, at <a href="http://developer.mugshot.org">http://developer.mugshot.org</a></li>
<li>A small scale, limited access user trial of the Mugshot service at <a href="http://mugshot.org">http://mugshot.org</a> </li>
</ul>

<p>
This is an early release, intended to open up the design and development
process to the open source community at an early stage.
</p>


<a name="7">
<h3>7. What's next?</h3>
   
<p>
We are currently in the research and design phase for a set of
features that center around creating a live social experience around
TV and video. 
</p>

<p>
As an open source project, Mugshot is open to participation from the 
community.  More information on design and development for future
capabilities is available at: <a href="http://developer.mugshot.org">http://developer.mugshot.org</a>
</p>


<a name="8">
<h3>8. How do I get involved?</h3>
   
<ul>
<li>Visit the developer site for information on how to participate in the open source design and development project: <a href="http://developer.mugshot.org">http://developer.mugshot.org</a></li>
<li>Sign up at <a href="http://mugshot.org">http://mugshot.org</a> to be notified when public accounts are available </li>
</ul>

<a name="9">
<h3>9. What is the relationship to existing social networking sites?</h3>
   
<p>
Mugshot is intended to work with "social networking" sites,
not replace them, and to add new live social experiences to them. For
example, Mugshot currently allows users to publish their iTunes song
history to their MySpace profile.
</p>

<p>
Unlike most social networking services, Mugshot is a completely 
open system -- including the server and client source code.
</p>


<a name="10">
<h3>10. What is the relationship to existing music services and applications?</h3>
    
<p>
Mugshot is not a replacement for existing online music services.  Instead,
it works with existing applications and services to add new live
social experiences to them. For example, Mugshot currently allows iTunes
users to share play lists with users of other services, and to
share play lists in new contexts, such as blogs or social network
profile pages.
</p>


<a name="11">
<h3>11. What is the relationship to existing instant messaging services?</h3>
    
<p>
Mugshot is not a replacement for existing IM services. Instead, we
intend for it to work with existing IM applications and services to
deliver new live social experiences.
</p>

<p>
Mugshot does make use of the industry standard Jabber/XMPP protocol
under the hood for real time communications between the server and
clients.
</p>


<a name="12">
<h3>12. What is the relationship to online services initiatives such as Microsoft Live and Apple's .Mac?</h3>
    
<p>
Mugshot shares some of the aims of these initiatives to integrate web
services more deeply into the everyday PC experience.
</p>

<p>
Rather than developing a new integrated suite of web services tuned to
a specific client operating system, Mugshot aims to provide a level
playing field for a variety of commercial and non-commercial service
providers, and to work with a variety of PC and device platforms.
</p>


<a name="13">
<h3>13. Is the music sharing feature legal?</h3>
    
<p>
Mugshot does not transfer music, it just provides pointers to legal
commercial and non-commercial service providers where users can obtain
music.
</p>


<a name="14">
<h3>14. How does Mugshot relate to Red Hat?</h3>
    
<p>
Two of Red Hat's core values are collaboration and freedom.  Mugshot is 
an experiment in applying Red Hat's philosophy of collaboration and freedom to
new types of content, beyond software and source code.
</p>


<a name="15">
<h3>15. How does Mugshot benefit Red Hat?</h3>
    
<p>
Technology developed in the Mugshot project may be incorporated into
current and future Red Hat products and services. For example, Red Hat
may incorporate live social experiences into Red Hat's client
products, or offer commercial services around future versions of the
Mugshot software.
</p>

<p>
There are not yet any formal plans to incorporate Mugshot into the Red
Hat Enterprise Linux or Fedora Core distributions.
</p>


<a name="16">
<h3>16. What platforms and services are supported?</h3>
    
<p>
The Mugshot client software is currently available for Windows XP and
Linux. The Mugshot web service also offers limited support for Apple
OS X and other platforms.
</p>

<p>
The Mugshot client software currently offers integration with Apple
iTunes, Yahoo! Music Engine, Internet Explorer and Firefox on Windows
XP. The open source Rhythmbox music player and Firefox are supported
on Linux.
</p>

<p>
We expect to broaden the platform to support other applications and
commercial and non-commercial service providers. Because Mugshot is an
open source project, outside developers can contribute support for new
applications and service providers.
</p>


<a name="17">
<h3>17. What web services does Mugshot use?</h3>
    
<p>
Mugshot currently makes use of public web services from Yahoo! and
Amazon, but has no formal partnership with either company related 
to this project.
</p>

<p>
We expect to make use of web services from other service providers and
interoperate with other applications in the future. 
</p>
    
</dht:systemPage>
</html>
