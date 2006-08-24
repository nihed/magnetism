package com.dumbhippo.live;

import com.dumbhippo.statistics.Column;
import com.dumbhippo.statistics.ColumnType;
import com.dumbhippo.statistics.ColumnUnit;
import com.dumbhippo.statistics.StatisticsSource;

/**
 * Statistics source with columns providing data about the LiveState object cache
 * @author otaylor
 */
public class LiveStatistics implements StatisticsSource {
	@Column(id="availableUserCount",
			name="Available User Count", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getAvailableUserCount() {
		return LiveState.getInstance().getLiveUserAvailableCount();
	}	

	@Column(id="cachedUserCount",
			name="Cached User Count", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getCachedUserCount() {
		return LiveState.getInstance().getLiveUserCount();
	}
	
	@Column(id="cachedPostCount",
			name="Cached Post Count", 
			units=ColumnUnit.COUNT, 
			type=ColumnType.SNAPSHOT)
	public long getCachePostCount() {
		return LiveState.getInstance().getLivePostCount();
	}	
}
