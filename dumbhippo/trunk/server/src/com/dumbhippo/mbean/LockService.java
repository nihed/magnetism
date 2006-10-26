package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBeanSupport;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannelFactory;
import org.jgroups.blocks.DistributedLockManager;
import org.jgroups.blocks.LockManager;
import org.jgroups.blocks.LockNotGrantedException;
import org.jgroups.blocks.LockNotReleasedException;
import org.jgroups.blocks.VotingAdapter;
import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.dumbhippo.GlobalSetup;

/**
 * This class provides a simple wrapper around org.jgroups.block.DistributedLockManager
 * 
 * @author otaylor
 */
public class LockService extends ServiceMBeanSupport implements LockServiceMBean {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LockService.class);
	
	static private final String CLUSTER_NAME = "LockService";
	
	// ---- Member variables -----

	private String clusterName = CLUSTER_NAME;
	private Element config;
	private Channel channel;
	private VotingAdapter votingAdapter;
	private LockManager lockManager;
	
	// ---- PresenceServiceMBean methods -----
	
	public void setClusterName(String name) {
		this.clusterName = name;
	}
	
	public String getClusterName() {
		return clusterName;
	}
	
	public void setClusterConfig(Element element) {
		this.config = element;
	}

	// ---- Life-cycle methods -----
	
	static private LockService instance;
	
	@Override
	protected void startService() {
		logger.info("Starting LockService");

		try {
			JChannelFactory factory = new JChannelFactory(config);
			channel = factory.createChannel();
			channel.connect(clusterName);
			
		} catch (ChannelException e) {
			throw new RuntimeException("Failed to create JGroups channel", e);
		}
		
		votingAdapter = new VotingAdapter(channel);
		lockManager = new DistributedLockManager(votingAdapter, channel.getLocalAddress());
		
		instance = this;
		
		logger.info("Started LockService");
	}

	@Override
	protected void stopService() {
		logger.info("Stopping PresenceService");

		instance = null;
		
		channel.close();
		channel = null;
		
		logger.info("Stopped LockService");
	}

	// ---- Public methods -----

	/**
	 * Return the global per-process singleton LockService object.
	 */
	public static LockService getInstance() {
		return instance;
	}

	/**
	 * Establish a cluster-wide lock on the given object. Although the JGroups
	 * LockManager interface allows specifying a separate owner per lock,
	 * we omit that parameter here because it turns out that that the JGroups
	 * lock manager implementation only handles node crashes correctly when
	 * the owner is the node address.
	 * 
	 * @param obj The object to lock (must be serializable)
	 * @param timeout timeout before giving up on trying to get the lock 
	 * @throws LockNotGrantedException if the lock wasn't granted (because of a timeout,
	 *    perhaps)
	 * @throws ChannelException if getting the lock failed because of a communication
	 *    problem between this node and other nodes. 
	 */
	public void lock(Object obj, int timeout) throws LockNotGrantedException, ChannelException {
		lockManager.lock(obj, channel.getLocalAddress(), timeout);
	}

	/**
	 * Releases a lock established with lock()
	 * 
	 * @param obj The object to lock (must be serializable)
	 * @param timeout timeout before giving up on trying to get the lock 
	 * @throws LockNotReleasedException if the lock wasn't (not clear why this
	 *   would happen)
	 * @throws ChannelException if releasing the lock failed because of a communication
	 *    problem between this node and other nodes. 
	 */
	public void unlock(Object obj) throws LockNotReleasedException, ChannelException {
		lockManager.unlock(obj, channel.getLocalAddress());
	}
}
