package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.blocks.NetflixBlockView;
import com.dumbhippo.services.NetflixMovieView;
import com.dumbhippo.services.NetflixMoviesView;

@DMO(classId="http://mugshot.org/p/o/netflixMovieBlock")
public abstract class NetflixMovieBlockDMO extends BlockDMO {
	protected NetflixMovieBlockDMO(BlockDMOKey key) {
		super(key);
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getImageUrl() {
		return ((NetflixBlockView)blockView).getImageUrl();
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public List<NetflixMovieDMO> getQueuedMovies() {
		NetflixBlockView netflixBlockView = (NetflixBlockView)blockView;
		User user = netflixBlockView.getPersonSource().getUser();
		List<NetflixMovieDMO> result = new ArrayList<NetflixMovieDMO>();
		
		NetflixMoviesView queuedMovies = netflixBlockView.getQueuedMovies();
		if (queuedMovies != null) {
			for (NetflixMovieView movie : queuedMovies.getMovies()) {
				result.add(session.findUnchecked(NetflixMovieDMO.class, NetflixMovieDMO.getKey(user, movie)));
			}
		}
		
		return result;
	}
}
