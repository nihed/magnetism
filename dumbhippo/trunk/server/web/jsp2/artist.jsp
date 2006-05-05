<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="musicsearch" class="com.dumbhippo.web.MusicSearchPage" scope="page"/>
<jsp:setProperty name="musicsearch" property="song" param="track"/>
<jsp:setProperty name="musicsearch" property="album" param="album"/>
<jsp:setProperty name="musicsearch" property="artist" param="artist"/>

<head>
	<title><c:out value="${musicsearch.expandedArtistView.name}"/></title>
    <link rel="stylesheet" type="text/css" href="/css2/artist.css"/>
	<dht:scriptIncludes/>	
</head>

<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxArtists>
            <c:choose>
		        <c:when test="${not empty musicsearch.expandedArtistView}">
                    <dht:artistProfile artist="${musicsearch.expandedArtistView}"/>
                    <dht:zoneBoxSeparator/>
                    <div class="dh-artist-zone-title">DISCOGRAPHY</div>
                    <c:choose>
                        <c:when test="${not empty musicsearch.bestAlbum}">
                            <dht:album album="${musicsearch.bestAlbum}"/>
                        </c:when>
                        <c:otherwise>
                            There were not matching albums.
                        </c:otherwise>
                    </c:choose>            
                    <%-- if we don't use something like this separator, zone background color comes --%>
                    <%-- under the displayed album --%>
                    <dht:zoneBoxSeparator visible="false"/>
                </c:when>
                <c:otherwise>
                    There were no matching results.
                </c:otherwise>
            </c:choose>    
        </dht:zoneBoxArtists>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>           
            