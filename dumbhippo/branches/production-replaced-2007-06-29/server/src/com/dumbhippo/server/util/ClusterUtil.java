package com.dumbhippo.server.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ha.framework.interfaces.HAPartition;

public class ClusterUtil {
	/**
	 * Gets the HAPartition for the cluster of servers. This is useful
	 * for finding out the internet address of the other servers in the
	 * cluster.
	 * 
	 * @return the HAPartition (throws a RuntimeException if for some reason
	 *   the object can't be found. It should always exist during server
	 *   operation.)
	 */
	static public HAPartition getPartition() {
		try {
			String partitionName = System.getProperty("jboss.partition.name");
			Context context = new InitialContext();
			return (HAPartition)context.lookup("/HAPartition/" + partitionName);
		} catch (NamingException e) {
			throw new RuntimeException("Can't look up HAPartition");
		}
	}
}
