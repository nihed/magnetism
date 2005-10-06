package com.dumbhippo.jive;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.messenger.user.User;
import org.jivesoftware.messenger.user.UserAlreadyExistsException;
import org.jivesoftware.messenger.user.UserNotFoundException;
import org.jivesoftware.messenger.user.UserProvider;

public class HippoUserProvider implements UserProvider {

	public User loadUser(String username) throws UserNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	public User createUser(String username, String password, String name,
			String email) throws UserAlreadyExistsException {
		throw new UnsupportedOperationException("Users must be created on the web site");
	}

	public void deleteUser(String username) {
		// TODO Auto-generated method stub

	}

	public int getUserCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Collection<User> getUsers() {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<User> getUsers(int startIndex, int numResults) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPassword(String username) throws UserNotFoundException,
			UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setPassword(String username, String password)
			throws UserNotFoundException, UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public void setName(String username, String name)
			throws UserNotFoundException {
		// TODO Auto-generated method stub

	}

	public void setEmail(String username, String email)
			throws UserNotFoundException {
		// TODO Auto-generated method stub

	}

	public void setCreationDate(String username, Date creationDate)
			throws UserNotFoundException {
		// TODO Auto-generated method stub

	}

	public void setModificationDate(String username, Date modificationDate)
			throws UserNotFoundException {
		// TODO Auto-generated method stub

	}

	public Set<String> getSearchFields() throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<User> findUsers(Set<String> fields, String query)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<User> findUsers(Set<String> fields, String query,
			int startIndex, int numResults)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

}
