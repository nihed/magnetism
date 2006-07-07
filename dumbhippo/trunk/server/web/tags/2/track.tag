<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="track" required="true" type="com.dumbhippo.server.TrackView"%>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean"%>
<%@ attribute name="albumArt" required="false" type="java.lang.Boolean"%>
<%@ attribute name="linkifySong" required="false" type="java.lang.Boolean"%>
<%@ attribute name="playItLink" required="false" type="java.lang.Boolean"%>
<%-- when we display a track as an album track, we display a track number, omit artist, --%>
<%-- and display download links even if they are all disabled --%>
<%@ attribute name="displayAsAlbumTrack" required="false" type="java.lang.Boolean"%>
<%@ attribute name="order" required="false" type="java.lang.Integer"%>
<%@ attribute name="songOrder" required="false" type="java.lang.Integer"%>
<%@ attribute name="displaySinglePersonMusicPlay" required="false" type="java.lang.Boolean"%>

<c:if test="${empty displayAsAlbumTrack}">
	<c:set var="displayAsAlbumTrack" value="false"/>
</c:if>

<c:if test="${empty albumArt}">
	<c:set var="albumArt" value="false"/>
</c:if>

<c:if test="${empty oneLine}">
	<c:set var="oneLine" value="false"/>
</c:if>

<c:if test="${empty linkifySong}">
    <c:choose>
        <c:when test="${displayAsAlbumTrack}">
	        <c:set var="linkifySong" value="false"/>        
        </c:when>
        <c:otherwise>
            <c:set var="linkifySong" value="true"/>
        </c:otherwise>
    </c:choose>    
</c:if>

<c:if test="${empty playItLink}">
	<c:set var="playItLink" value="true"/>
</c:if>

<c:if test="${empty displaySinglePersonMusicPlay}">
	<c:set var="displaySinglePersonMusicPlay" value="false"/>
</c:if>

<c:url value="/artist" var="albumlink">
	<c:param name="artist" value="${track.artist}"/>
	<c:param name="album" value="${track.album}"/>
</c:url>

<c:url value="/artist" var="artistlink">
	<c:param name="artist" value="${track.artist}"/>
</c:url>

<c:choose>
	<c:when test="${albumArt}">
		<c:set var="songClass" value="dh-song dh-song-with-art" scope="page"/>
	</c:when>
	<c:when test="${oneLine}">
		<c:set var="songClass" value="dh-song dh-one-line-song" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="songClass" value="dh-song" scope="page"/>
	</c:otherwise>
</c:choose>

<c:if test="${displayAsAlbumTrack}">    
    <c:choose>
        <c:when test="${track.showExpanded}">
            <c:set var="songPlaysOpenedClass" value=""/> 	
            <c:set var="songPlaysClosedClass" value="dh-invisible"/>                            
        </c:when>  
        <c:otherwise>
            <c:set var="songPlaysOpenedClass" value="dh-invisible"/>   
            <c:set var="songPlaysClosedClass" value=""/>                         
        </c:otherwise>  
    </c:choose>
</c:if>            
            
<div class='${songClass}'>
	<c:if test="${albumArt}">
		<div class="dh-song-image">
			<a href="${albumlink}">
				<img src="${track.smallImageUrl}" width="${track.smallImageWidth}" height="${track.smallImageHeight}"/>
			</a>
		</div>
	</c:if>
	<div class="dh-song-info">
		<c:if test="${!empty track.name}">
			<div class="dh-song-name">
				<c:if test="${displayAsAlbumTrack}">
                    <c:out value="${track.trackNumber} "/>
                </c:if> 			
				<c:if test="${linkifySong}">
					<c:url value="/artist" var="songlink">
						<c:param name="track" value="${track.name}"/>
						<c:param name="artist" value="${track.artist}"/>
						<c:param name="album" value="${track.album}"/>
					</c:url>
					<a href="${songlink}">
				</c:if>
				<c:choose>
		            <c:when test="${displayAsAlbumTrack}"> 
		                <span id="dhSongNamePlaysClosed${order}s${songOrder}" class="${songPlaysClosedClass}">
		                    <a href="javascript:dh.artist.openSongPlays(${order}, ${songOrder});">
					        <c:out value="${track.truncatedName}"/>
					        </a>
					    </span>
		                <span id="dhSongNamePlaysOpened${order}s${songOrder}" class="${songPlaysOpenedClass}">
		                    <a href="javascript:dh.artist.closeSongPlays(${order}, ${songOrder});">
		                    <img src="/images2/${buildStamp}/arrow_down.gif"/>
					        <c:out value="${track.truncatedName}"/>
					        </a>
					    </span>
					</c:when>
					<c:otherwise> 
					    <c:out value="${track.name}"/>
					</c:otherwise>
				</c:choose>	    					   
				<c:if test="${linkifySong}">
					</a>
				</c:if>
			</div>
		</c:if>
		<c:if test="${!empty track.artist && !displayAsAlbumTrack}">
			<div class="dh-song-artist">
				by
				<a href="${artistlink}">
					<c:out value="${track.artist}"/>
				</a>
			</div>
		</c:if>
		<c:if test="${playItLink}">
		    <c:if test="${!empty track.itunesUrl || !empty track.yahooUrl || !empty track.rhapsodyUrl}">
				<c:set var="itunesDisabled" value='${empty track.itunesUrl ? "disabled" : ""}'/>
				<c:set var="yahooDisabled" value='${empty track.yahooUrl ? "disabled" : ""}'/>
				<c:set var="rhapsodyDisabled" value='${empty track.rhapsodyUrl ? "disabled" : ""}'/>
				<div class="dh-song-links">Play at 
					<c:if test="${!empty track.itunesUrl}">
						<a class="dh-music-source-link" href="${track.itunesUrl}">iTunes</a>
						<c:if test="${!empty track.yahooUrl || !empty track.rhapsodyUrl}">
					        ,
					    </c:if>
					</c:if>
					<c:if test="${!empty track.yahooUrl}">
						<a class="dh-music-source-link" href="${track.yahooUrl}">Yahoo! Music</a>
						<c:if test="${!empty track.rhapsodyUrl}">
					        ,
					    </c:if>
					</c:if>
					<c:if test="${!empty track.rhapsodyUrl}">
						<a class="dh-music-source-link" href="${track.rhapsodyUrl}">Rhapsody</a>
					</c:if>
				</div>
			</c:if>
		</c:if>
		<c:if test="${displayAsAlbumTrack}">
		    <div id="dhSongPlaysOpened${order}s${songOrder}" class="${songPlaysOpenedClass}">
		        <c:set var="songPlaysClass" value="dh-song-plays"/>
		        <c:if test="${track.trackNumber >= 10}">
		            <c:set var="songPlaysClass" value="dh-song-plays dh-double-digit-track-song-plays"/>           
                </c:if>
                <div class="${songPlaysClass}">
		            <c:choose>
		                <c:when test="${track.totalPlays == 0}">
		                    0 plays
		                </c:when>    
    		            <c:when test="${track.totalPlays == 1}">
	    	                1 play
		                </c:when>
		                <c:otherwise>    
		  	                <c:out value="${track.totalPlays}"/> total plays
		  	            </c:otherwise>
    		  	    </c:choose>    
    		  	    <%-- if we are looking at the page from a system viewpoint, track.numberOfFriendsWhoPlayedTrack --%> 
    		  	    <%-- will be -1, and we will not display this --%> 
    		  	    <c:if test="${track.totalPlays > 0 && track.numberOfFriendsWhoPlayedTrack >= 0}">
    		  	        <br/>
    		  	        <c:choose>
    		  	            <c:when test="${track.numberOfFriendsWhoPlayedTrack == 1}">
    		  	                1 friend played this
    		  	            </c:when>
    		  	            <c:otherwise>
    		  	                <c:out value="${track.numberOfFriendsWhoPlayedTrack}"/> friends played this
    		  	            </c:otherwise>
    		  	        </c:choose>   	
    		  	    </c:if>    	  	                  
	    	  	    <ul>
		     	    <c:forEach items="${track.personMusicPlayViews}" var="personMusicPlay">
		  	            <li>
		                <div class="dh-song-plays-person">		            
                            <dht:personName who="${personMusicPlay.person}" identifySelf="true"/>
                            played this on
                            <c:out value="${personMusicPlay.formattedLastPlayed}"/>
                        </div>
                        </li>    
                    </c:forEach>
                    </ul>
                </div>
		  	</div>      
		</c:if>		
		<c:if test="${displaySinglePersonMusicPlay && !(empty track.singlePersonMusicPlayView)}">
		    <br/>
		    <div class="dh-song-plays-person">	
		        played by <dht:personName who="${track.singlePersonMusicPlayView.person}" identifySelf="true"/>
		    </div>  
		</c:if>    
	</div>
	<c:if test="${albumArt}">
		<div class="dh-grow-div-around-floats"></div>
	</c:if>
</div>
