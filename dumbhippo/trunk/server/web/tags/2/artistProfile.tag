<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="artist" required="true" type="com.dumbhippo.server.ExpandedArtistView"%>

<div class="dh-artist-profile">
    <div class="dh-artist-image">
        <img src="${artist.smallImageUrl}" width="${artist.smallImageWidth}" height="${artist.smallImageHeight}"/>
    </div>       
    <div class="dh-artist-info">      
        <div class="dh-artist-name">
            <!-- TODO: do we want to make sure that artist name is all caps like other zone titles? -->
            <!-- What should happen with the page title then? -->
            <c:out value="${artist.name}"/>
        </div>
        <div class="dh-artist-bio">
            We will soon be displaying the artist bio. For now, you can read about 
		    the artist <a href="${artist.yahooMusicPageUrl}" target="_blank">here</a>.
	    </div>
	</div>    
</div>
