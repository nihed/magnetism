package com.dumbhippo.server.dm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.NetflixMovieView;
import com.dumbhippo.services.NetflixMoviesView;
import com.dumbhippo.services.caches.NetflixQueueMoviesCache;
import com.dumbhippo.services.caches.WebServiceCache;

@DMO(classId="http://mugshot.org/p/o/netflixMovie", resourceBase="/o/netflixMovie")
public abstract class NetflixMovieDMO extends DMObject<NetflixMovieKey> {
	private NetflixMovieView movie;
	
	@WebServiceCache
	private NetflixQueueMoviesCache moviesCache;
	
	@EJB
	private ExternalAccountSystem externalAccountSystem;

	@EJB
	private IdentitySpider identitySpider;

	protected NetflixMovieDMO(NetflixMovieKey key) {
		super(key);
	}

	private static Pattern NETFLIX_URL_PATTERN = Pattern.compile("http://www.netflix.com/Movie/(?:[A-Za-z0-9_]+)/([0-9]+)(?:\\?.*)?");
	
	private static String extractExtra(String url) {
		Matcher m = NETFLIX_URL_PATTERN.matcher(url);
		if (m.matches())
			return m.group(1);
		else
			throw new RuntimeException("Cannot extract key from Netflix URL '" + url + "'");
	}
	
	@Override
	protected void init() throws NotFoundException {
		movie = (NetflixMovieView)getKey().getObject();
		if (movie != null)
			return;
		
		User user = identitySpider.lookupUser(getKey().getUserId());
		
		ExternalAccount netflixAccount = 
			externalAccountSystem.lookupExternalAccount(SystemViewpoint.getInstance(), user, ExternalAccountType.NETFLIX);
		
		NetflixMoviesView moviesView = moviesCache.getSync(netflixAccount.getHandle());
		for (NetflixMovieView m : moviesView.getMovies()) {
			if (extractExtra(m.getUrl()).equals(getKey().getExtra())) {
				movie = m;
			}
		}
		
		if (movie == null)
			throw new NotFoundException("Movie not found for user=" + user + " extra=" + getKey().getExtra());
	}
	
	@DMProperty(defaultInclude=true)
	public String getDescription() {
		return movie.getDescription();
	}
	
	@DMProperty(defaultInclude=true)
	public int getPriority() {
		return movie.getPriority();
	}
	
	@DMProperty(defaultInclude=true)
	public String getTitle() {
		return movie.getTitle();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getUrl() {
		return movie.getUrl();
	}

	public static NetflixMovieKey getKey(User user, NetflixMovieView movie) {
		return new NetflixMovieKey(user.getGuid(), extractExtra(movie.getUrl()), movie); 
	}
}
