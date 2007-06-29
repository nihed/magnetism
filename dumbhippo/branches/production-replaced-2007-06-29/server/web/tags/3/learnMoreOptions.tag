<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>


<%@ attribute name="exclude" required="false" type="java.lang.String" %>

<div>Learn more about: 
<c:if test="${signin.valid}">
     <a class="dh-underlined-link" href="download">Installing the download</a> |
</c:if>     
<c:if test="${exclude != 'webSwarm'}">
    <a class="dh-underlined-link" href="links-learnmore">Web Swarm</a> | 
</c:if>
<c:if test="${exclude != 'musicRadar'}">
    <a class="dh-underlined-link" href="radar-learnmore">Music Radar</a> | 
</c:if>
<c:if test="${exclude != 'stacker'}">
    <a class="dh-underlined-link" href="stacker-learnmore">The Stacker</a> 
</c:if>
<c:if test="${exclude != 'stacker' && exclude != 'webAccounts'}">
    | 
</c:if>
<c:if test="${exclude != 'webAccounts'}">
    <a class="dh-underlined-link" href="web-accounts-learnmore">Web Accounts</a> 
</c:if>
</div>
