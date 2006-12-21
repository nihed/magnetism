<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>

<dh:script module="dh.infoviewer"/>
<div class="dh-person-item">
    <div id="dhImage${who.identifyingGuid}" class="dh-image">
	    <dht:headshot person="${who}" size="60" includePresence="true"/>
    </div>
    <div class="dh-person-item-name">
        <c:if test="${who.liveUser != null}">
            <a href="${who.homeUrl}">
       </c:if>
       <c:out value="${who.truncatedName}"/>
       <c:if test="${who.liveUser != null}">       
           </a>
       </c:if>    
    </div>
</div>    
<div id="dhInfo${who.identifyingGuid}" class="dh-person-item-more-info">
	<table cellpadding="0" cellspacing="0">
	<tbody>
	<tr valign="top">
	<td>
        <div class="dh-image">
	        <dht:headshot person="${who}" size="60"/>
        </div>   
    </td>
    <td>
        <c:if test="${who.liveUser != null}">  
            <dht3:presenceIcon who="${who}"/>
        </c:if>    
        <span class="dh-person-item-name">
            <c:out value="${who.name}"/>	
         </span>    
        <c:if test="${who.liveUser != null}">
		     <div class="dh-person-header-stats">
		        <span class="dh-info"><c:out value="${who.liveUser.userContactsCount} in network"/></span> | 							
			    <span class="dh-info"><dht3:plural n="${who.liveUser.groupCount}" s="group"/></span> | 
	            <span class="dh-info"><dht3:plural n="${who.liveUser.sentPostsCount}" s="post"/></span> 
		    </div>
        </c:if>		
    </td>
    </tr>
    </tbody>
    </table>    
</div>      
<script type="text/javascript">
	var imageDiv = document.getElementById("dhImage${who.identifyingGuid}");
	imageDiv.dhImageId = "${who.identifyingGuid}";
	imageDiv.onmouseover = dh.infoviewer.onImageMouseOver;
	imageDiv.onmouseout = dh.infoviewer.onImageMouseOut;
	var infoDiv = document.getElementById("dhInfo${who.identifyingGuid}");
	infoDiv.dhInfoId = "${who.identifyingGuid}";
	infoDiv.onmouseout = dh.infoviewer.onInfoMouseOut;
</script>
