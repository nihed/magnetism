package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.listeners.AccountStatusListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.server.listeners.GroupCreationListener;
import com.dumbhippo.server.listeners.GroupMembershipListener;
import com.dumbhippo.server.listeners.PostListener;
import com.dumbhippo.server.listeners.UserCreationListener;
import com.dumbhippo.server.util.EJBUtil;

/**
 * See the docs for the Notifier interface.
 * 
 * The entire bean has TransactionAttributeType.SUPPORTS
 * because there's no reason for NotifierBean to modify the 
 * transaction state; the notifying code or notified code might
 * care, but they can set their own transaction attributes.
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class NotifierBean implements Notifier {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(NotifierBean.class);
	
	// map listener interfaces to JNDI names of beans that implement them
	static private Map<Class<?>,List<String>> listenerBeanNames;
	
	static private Set<Class<?>> scanListenerInterfaces() {
		// We just scan what Notifier implements - simplest thing
		// I can think of for this
		Set<Class<?>> ifaces = new HashSet<Class<?>>();
		for (Class<?> iface : Notifier.class.getInterfaces()) {
			ifaces.add(iface);
		}
		return ifaces;
	}
	
	static private Collection<String> scanSessionBeans() {
		Collection<String> beanNames;
		try {
			beanNames = EJBUtil.listLocalBeanNames();
			return beanNames;
		} catch (NamingException e) {
			logger.warn("Naming exception listing bean classes", e);
			return Collections.emptyList();
		}
	}
	
	static synchronized private void scan() {
		if (listenerBeanNames != null)
			return;
		
		listenerBeanNames = new HashMap<Class<?>, List<String>>();
		
		Collection<String> beanNames = scanSessionBeans();
		Set<Class<?>> listenerIfaces = scanListenerInterfaces();
		for (String beanName : beanNames) {
			Class<?> beanIface =
				EJBUtil.loadLocalBeanInterface(NotifierBean.class.getClassLoader(),
					beanName);

			if (beanIface == null) {
				logger.debug("No interface loaded for bean {} (probably because it's @Remote)", beanName);
				continue;
			}
			
			// this would be bad to miss
			if (beanIface.equals(Notifier.class))
				continue;
			
			for (Class<?> listenerIface : listenerIfaces) {
				if (listenerIface.isAssignableFrom(beanIface)) {
					List<String> list = listenerBeanNames.get(listenerIface);
					if (list == null) {
						list = new ArrayList<String>();
						listenerBeanNames.put(listenerIface, list);
					}
					
					logger.debug("Notifier adding bean {} implementing listener {}",
							beanName, listenerIface.getName());
					
					list.add(beanName);
				}
			}
		}
	}
	
	// cache of jndi name to proxy object
	private Map<String,Object> beans;
	// cache of listener interface to proxy objects
	private Map<Class<?>,List<?>> listeners;
	
	// a hook to be sure the static initialization is done before we do 
	// anything else but after the JNDI context is ready.
	// Avoids having to check static init in every method.
	@PostConstruct
	public void init() {
		scan();
		beans = new HashMap<String,Object>();
		listeners = new HashMap<Class<?>,List<?>>();
	}
	
	private <T> T getBean(Class<? extends T> klass, String name) {
		T bean = klass.cast(beans.get(name));
		if (bean == null) {
			try {
				Context ctx = new InitialContext();
				bean = klass.cast(ctx.lookup(name));
			} catch (NamingException e) {
				logger.warn("Failed to lookup bean {} implementing {}",
						name, klass.getName());
				throw new RuntimeException(e);
			}
			beans.put(name, bean);
		}
		return bean;
	}
	
	private <T> List<T> getListeners(Class<T> listenerClass) {
		
		List<?> listUnknown = listeners.get(listenerClass);
		
		if (listUnknown != null) {
			return TypeUtils.castList(listenerClass, listUnknown);
		}	
		
		List<T> list = new ArrayList<T>();
		
		List<String> beanNames = listenerBeanNames.get(listenerClass);
		if (beanNames != null) {
			for (String name : beanNames) {
				T bean = getBean(listenerClass, name);
				list.add(bean);
			}
		}
		
		listeners.put(listenerClass, list);
		
		return list;
	}
	
	public void onUserCreated(User user) {
		for (UserCreationListener l : getListeners(UserCreationListener.class)) {
			l.onUserCreated(user);
		}
	}

	public void onAccountDisabledToggled(Account account) {
		for (AccountStatusListener l : getListeners(AccountStatusListener.class)) {
			l.onAccountDisabledToggled(account);
		}
	}

	public void onAccountAdminDisabledToggled(Account account) {
		for (AccountStatusListener l : getListeners(AccountStatusListener.class)) {
			l.onAccountAdminDisabledToggled(account);
		}
	}

	public void onMusicSharingToggled(Account account) {
		for (AccountStatusListener l : getListeners(AccountStatusListener.class)) {
			l.onMusicSharingToggled(account);
		}
	}

	public void onGroupCreated(Group group) {
		for (GroupCreationListener l : getListeners(GroupCreationListener.class)) {
			l.onGroupCreated(group);
		}
	}

	public void onPostCreated(Post post) {
		for (PostListener l : getListeners(PostListener.class)) {
			l.onPostCreated(post);
		}
	}

	public void onPostDisabledToggled(Post post) {
		for (PostListener l : getListeners(PostListener.class)) {
			l.onPostDisabledToggled(post);
		}
	}

	public void onGroupMemberCreated(GroupMember member) {
		for (GroupMembershipListener l : getListeners(GroupMembershipListener.class)) {
			l.onGroupMemberCreated(member);
		}
	}

	public void onExternalAccountCreated(User user, ExternalAccount external) {
		for (ExternalAccountsListener l : getListeners(ExternalAccountsListener.class)) {
			l.onExternalAccountCreated(user, external);
		}
	}
}
