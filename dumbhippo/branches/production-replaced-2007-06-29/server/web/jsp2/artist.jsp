<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="musicsearch" class="com.dumbhippo.web.pages.MusicSearchPage" scope="page"/>
<jsp:setProperty name="musicsearch" property="song" param="track"/>
<jsp:setProperty name="musicsearch" property="album" param="album"/>
<jsp:setProperty name="musicsearch" property="artist" param="artist"/>

<head>
	<title><c:out value="${musicsearch.expandedArtistView.name}"/></title>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/artist.css"/>
	<!--[if IE]>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/artist-iefixes.css">
	<![endif]-->
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>	
</head>

<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxArtists>
            <c:choose>
		        <c:when test="${not empty musicsearch.expandedArtistView}">
                    <dht:artistProfile artist="${musicsearch.expandedArtistView}"/>
                    <dht:zoneBoxSeparator/>
                    <a name="dhAlbumsByArtist"></a>
                    <div class="dh-artist-zone-title">DISCOGRAPHY</div>
                    <c:if test="${musicsearch.albumsByArtist.resultCount <= 0}">
                        We couldn't find any albums related to <c:out value="${musicsearch.searchDescription}"/>.
                    </c:if>            
                       
                    <c:set var="count" value="1"/>   
                    <c:forEach items="${musicsearch.albumsByArtist.results}" var="album">
					    <dht:album album="${album}" order="${count}"/>
					    <c:set var="count" value="${count+1}"/>
					</c:forEach>
	
	                <div class="dh-artist-more">
					    <dht:expandablePager pageable="${musicsearch.albumsByArtist}" anchor="dhAlbumsByArtist"/>					    
					</div>
					<%-- if we don't use something like this separator, zone background color comes --%>
                    <%-- under the displayed album --%>
                    <dht:zoneBoxSeparator visible="false"/>
                </c:when>
                <c:when test="${empty musicsearch.artist && empty musicsearch.album && empty musicsearch.song}">
                	How'd you get here? This page should have an artist, album, or song name in its link.
				</c:when>
                <c:otherwise>
                	We couldn't find any information for <c:out value="${musicsearch.searchDescription}"/>.
                	<c:if test="${!empty musicsearch.artist && (!empty musicsearch.album || !empty musicsearch.song)}">
                		<div>
                			<a href="${musicsearch.artistOnlyUrl}">Try just the artist name '<c:out value="${musicsearch.artist}"/>'</a>.
                		</div>
                	</c:if>
                	<div>
	                	Try
                		<c:if test="${!empty musicsearch.lastFmArtistUrl}">
							<a href="${musicsearch.lastFmArtistUrl}" target="_blank">'<c:out value="${musicsearch.artist}"/>' on last.fm</a>, or
                		</c:if>
    	            	<a href="${musicsearch.yahooSearchUrl}" target="_blank">Yahoo! audio search</a>
    	            </div>
                </c:otherwise>
            </c:choose>
        </dht:zoneBoxArtists>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>           
            