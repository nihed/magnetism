package com.dumbhippo.jive;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.messenger.user.User;
import org.jivesoftware.messenger.user.UserAlreadyExistsException;
import org.jivesoftware.messenger.user.UserNotFoundException;
import org.jivesoftware.messenger.user.UserProvider;

import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MessengerGlueRemote.JabberUser;

public class HippoUserProvider implements UserProvider {

	private UserNotFoundException createUserNotFound(String username, Exception root) {
		return new UserNotFoundException ("No account has username '" + username + "'", root);
	}
	
	public User loadUser(String username) throws UserNotFoundException {
		MessengerGlueRemote glue = Server.getMessengerGlue();
		
		JabberUser remote = null;
		try {
			remote = glue.loadUser(username);
		} catch (JabberUserNotFoundException e) {
			throw createUserNotFound(username, e);
		}
		
		return new User(username, remote.getName(), remote.getEmail(), null, null);
	}

	public User createUser(String username, String password, String name,
			String email) throws UserAlreadyExistsException {
		throw new UnsupportedOperationException("Users must be created on the web site");
	}

	public void deleteUser(String username) {
		throw new UnsupportedOperationException("Users must be deleted on the web site");
	}

	public int getUserCount() {
		MessengerGlueRemote glue = Server.getMessengerGlue();
	
		long result = glue.getJabberUserCount();
		// Is there such a thing as optimistic paranoia?
		if (result > Integer.MAX_VALUE)
			throw new Error("Too many users for JiveMessenger's mind!");
		return (int) result;
	}

	public Collection<User> getUsers() {
		// Whatever is calling this should be replaced by a call to 
		// our server which does a database query or something instead...
		throw new UnsupportedOperationException("Bug! getUsers() was called; we can't implement this, there are too many, so the caller will need to be changed");
	}

	public Collection<User> getUsers(int startIndex, int numResults) {
		// At the moment, this function is never used in the JiveMessenger source.
		throw new UnsupportedOperationException("Bug! Incremental getUsers() called; but it's not implemented yet");
	}

	public String getPassword(String username) throws UserNotFoundException,
			UnsupportedOperationException {
		throw new UnsupportedOperationException("Can't get password, have to use digest");
	}

	public void setPassword(String username, String password)
			throws UserNotFoundException, UnsupportedOperationException {
		throw new UnsupportedOperationException("You have to set your password on the web site");
	}

	public void setName(String username, String name)
			throws UserNotFoundException {
		MessengerGlueRemote glue = Server.getMessengerGlue();
		try {
			glue.setName(username, name);
		} catch (JabberUserNotFoundException e) {
			throw createUserNotFound(username, e);
		}
	}

	public void setEmail(String username, String email)
			throws UserNotFoundException {
		MessengerGlueRemote glue = Server.getMessengerGlue();
		try {
			glue.setEmail(username, email);
		} catch (JabberUserNotFoundException e) {
			throw createUserNotFound(username, e);
		}
	}

	public void setCreationDate(String username, Date creationDate)
			throws UserNotFoundException {
		throw new UnsupportedOperationException();
	}

	public void setModificationDate(String username, Date modificationDate)
			throws UserNotFoundException {
		throw new UnsupportedOperationException();

	}

	public Set<String> getSearchFields() throws UnsupportedOperationException {
		// TODO should probably implement this sometime
		throw new UnsupportedOperationException();
	}

	public Collection<User> findUsers(Set<String> fields, String query)
			throws UnsupportedOperationException {
		//		 TODO should probably implement this sometime
		throw new UnsupportedOperationException();
	}

	public Collection<User> findUsers(Set<String> fields, String query,
			int startIndex, int numResults)
			throws UnsupportedOperationException {
		//		 TODO should probably implement this sometime
		throw new UnsupportedOperationException();
	}

	public boolean isReadOnly() {
		// We don't support a lot of the modification operations, so maybe we should 
		// set this to true. But we do support a couple of them, so...
		return false;
	}

}
