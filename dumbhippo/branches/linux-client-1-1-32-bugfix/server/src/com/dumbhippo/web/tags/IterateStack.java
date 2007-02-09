package com.dumbhippo.web.tags;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.jsp.jstl.core.LoopTagSupport;

import com.dumbhippo.server.blocks.MusicBlockView;
import com.dumbhippo.server.views.TrackView;

/**
 * This tag is much like c:forEach, but it has special processing to suppress 
 * duplicate blocks in some circumstances.
 * 
 * @author otaylor
 */
public class IterateStack extends LoopTagSupport {
	private static final long serialVersionUID = 1L;
	
	Object items;
	Iterator<?> iterator;
	Object nextObject = null;
	Set<String> seenPlayIds = null;
	
	private boolean checkVisible(Object object) {
		String playId = null;
		if (object instanceof MusicBlockView) {
			TrackView track = ((MusicBlockView)object).getTrack();
			playId = track.getPlayId();
		}
		
		if (playId != null) {
			if (seenPlayIds.contains(playId))
				return false;
			
			seenPlayIds.add(playId);
		}
		
		return true;
	}
	
	private boolean ensureNextObject() {
		if (nextObject != null)
			return true;
		
		while (iterator.hasNext()) {
			Object object = iterator.next();
			if (checkVisible(object)) {
				nextObject = object;
				return true;
			}
		}
		
		return false;
	}

	public void setItems(Object items) {
		if (!(items instanceof Iterable)) {
			throw new IllegalArgumentException("Only iteration over Iterable is supported");
		}
		
		this.items = items;
	}
	
	@Override
	protected boolean hasNext() {
		return ensureNextObject();
	}

	@Override
	protected Object next() {
		if (!ensureNextObject()) 
			throw new NoSuchElementException();

		Object result = nextObject;
		nextObject = null;
		
		return result;
	}

	@Override
	protected void prepare() {
		Iterable iterable = (Iterable<?>)items;
		iterator = iterable.iterator();
		
		seenPlayIds = new HashSet<String>();
	}
}
