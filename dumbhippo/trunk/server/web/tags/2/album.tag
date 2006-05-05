<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="album" required="true" type="com.dumbhippo.server.AlbumView"%>

<div class="dh-album">
    <div class="dh-album-image">
        <img src="${album.smallImageUrl}" width="${album.smallImageWidth}" height="${album.smallImageHeight}"/>
    </div>       
    <div class="dh-album-info">      
        <div class="dh-album-title">
            <c:if test="${!empty album.title}">
                <c:out value="${album.truncatedTitle}"/>
            </c:if>
        </div>
        <div class="dh-album-year">
            <c:if test="${album.releaseYear > 0}">
                (<c:out value="${album.releaseYear}"/>)
            </c:if>
	    </div>
	    <dht:moreExpander open="true" text="songs"/>
	    <%-- the tracks list bellow is a List, not a ListBean, because it is --%>
        <%-- returned from outside the web tier due to double inderection --%>
	    <c:set var="firstTrack" value=" dh-first-track"/>
	    <c:set var="lastTrack" value=""/>
	    <c:set var="count" value="1"/>
        <c:forEach items="${album.tracks}" var="track">
            <c:if test="${count == album.numberOfTracks}">
	            <c:set var="lastTrack" value=" dh-last-track"/>                
            </c:if>            
            <c:choose>
                <c:when test="${count mod 2 == 0}">
	                <c:set var="trackParity" value=" dh-even-track"/>                
                </c:when>
                <c:otherwise>
	                <c:set var="trackParity" value=" dh-odd-track"/>                  
                </c:otherwise>
            </c:choose>                  
            <div class="dh-album-track${trackParity}${firstTrack}${lastTrack}">            
                <dht:track track="${track}" displayAsAlbumTrack="true" playItLink="true"/>								
            </div>    
            <c:set var="firstTrack" value=""/>
            <c:set var="count" value="${count+1}"/>
        </c:forEach>   
	</div>    
</div>