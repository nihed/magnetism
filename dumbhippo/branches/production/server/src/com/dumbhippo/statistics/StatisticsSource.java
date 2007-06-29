package com.dumbhippo.statistics;

/**
 * This interface should be implemented by any source of statistics data.
 * Right now the interface is empty, but conceivably methods like start()
 * and stop() could be added to it if necessary. The actual return of 
 * data is done by annotating getter methods with a long return value
 * with the {@link Column} annotation. 
 * 
 * @author otaylor
 */ 
public interface StatisticsSource {
}
