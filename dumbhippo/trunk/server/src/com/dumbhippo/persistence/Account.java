package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.util.EJBUtil;


/**
 * Object representing a Hippo account. Lots of things you might expect to be
 * here are not, because they're instead handled by saying that the Person in
 * question owns a Resource (see AccountClaim). So e.g. you your email address, home page, 
 * and other kinds of profile information.
 * If something can be in someone's contacts list without corresponding
 * to a registered Hippo user, then it can't be in the Account.
 * 
 * Putting things in the hippo account is a little subjective, but roughly we
 * store things here if nobody but the account holder would have an opinion on
 * them. Fields in Person, on the other hand, may be shared among multiple 
 * account holders since the Person is a common object. Another way to
 * put it is that things in the Hippo account require registration with our
 * system, while you can have a Person in your contacts list with full name,
 * email, aim ID, etc. all without having that person registered.
 * 
 * @author hp
 * 
 */
@Entity
public class Account extends Resource {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(Account.class);	
	
	private static final long serialVersionUID = 0L;
		
	private User owner;
	
	private long creationDate;
	private long lastLoginDate;
	private long lastLogoutDate;
	private long lastWebActivityDate;
	private int invitations;
	
	private boolean wasSentShareLinkTutorial;
	private boolean hasDoneShareLinkTutorial;
	
	private boolean hasAcceptedTerms;
	private boolean needsDownload;
	
	private boolean disabled;
	private boolean adminDisabled;
	
	// a hex-encoded SHA-1 hash of the password is stored
	private String password;
	private Integer passwordSalt;
	
	private Boolean musicSharingEnabled;
	// whether we've "primed" music sharing with some sample music
	private boolean musicSharingPrimed;
	private long nativeMusicSharingTimestamp;
	
	private Boolean notifyPublicShares;
	
	private NowPlayingTheme nowPlayingTheme;
	
	private Set<Post> favoritePosts;
	
	private String bio;
	private String musicBio;
	
	// Records the last time the user viewed their /group page, essentially
	private long lastSeenGroupInvitations;
	// The last time an invitation to a group was sent to this account; 
	// used to notify when we have new ones
	private long groupInvitationReceived;
	
	private String stackFilter;
	
	/*
	 * don't add accessors to this directly, we don't want clients to "leak"
	 * very far since they have auth keys. Instead add methods that do whatever
	 * you need to do.
	 */
	/* FIXME we can probably simplify this down to just one cookie right in the Account ? */
	private Set<Client> clients;

	private Set<ExternalAccount> externalAccounts; 
	
	/**
	 * Used only for Hibernate 
	 */
	protected Account() {
		this(null);
	}
	
	public Account(User owner) {			
		clients = new HashSet<Client>();
		favoritePosts = new HashSet<Post>();
		externalAccounts = new HashSet<ExternalAccount>();
		creationDate = -1;
		lastLoginDate = -1;
		lastLogoutDate = -1;
		lastWebActivityDate = -1;
		wasSentShareLinkTutorial = false;
		hasDoneShareLinkTutorial = false;
		needsDownload = true;
		nativeMusicSharingTimestamp = -1;
		disabled = false;
		musicSharingPrimed = false;
		lastSeenGroupInvitations = -1;
		groupInvitationReceived = -1;	
		// Do not initialize musicSharingEnabled here, keep it null intil the
		// user sets it one way or another.

		if (owner != null) {
			this.owner = owner;
			owner.setAccount(this);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{Account " + getId() + " owner = ");
		if (owner != null)
			builder.append(owner.toString());
		else 
			builder.append("null");
		
		builder.append(" disabled = " + disabled);
		
		builder.append("}");
		// Don't dump clients/contacts here - otherwise we won't be
		// able to toString() a detached account
		
		return builder.toString();
	}
	
	/**
	 * Bug work around, see docs for EJBUtil.forceInitialization()
	 */
	public void prepareToAuthorizeClient() {
		EJBUtil.forceInitialization(clients);
	}
	
	/** 
	 * Adds a new client as authorized to login to the 
	 * account without any other password, etc. 
	 * Note, there is deliberately no API to 
	 * retrieve an already-existing client. We can create
	 * a new one only (though the new one is just as good for logging 
	 * in as the old ones)
	 *   
	 * @param client the new client to auth
	 */
	public void authorizeNewClient(Client client) {
		clients.add(client);
	}
	
	/**
	 * Checks whether a per-client authorization cookie exists and 
	 * has not expired.
	 * 
	 * @param authKey
	 * @return true if the cookie is OK for authorization
	 */
	public boolean checkClientCookie(String authKey) {		
		for (Client client : clients) {
			String validKey = client.getAuthKey();
			//logger.debug("comparing provided key \"{}\" to valid key \"{}\"", authKey, validKey);
			if (validKey.equals(authKey))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks whether a per-client auth cookie exists and 
	 * has not expired, by prepending the given token 
	 * to the auth key, doing a SHA-1 hash, converting it 
	 * to an all-lowercase hex string, and seeing if it's 
	 * equal to the given digest.
	 * 
	 * @param token bytes to prepend to the digest
	 * @param digest the hex-encoded SHA-1 digest
	 * @return true if the cookie is OK for authorization
	 */
	public boolean checkClientCookie(String token, String digest) {
		for (Client client : clients) {
			String expectedDigest = Digest.computeDigest(token, client.getAuthKey());
			if (expectedDigest.equals(digest))
				return true;
		}
		return false;
	}
	
	/**
	 * This is protected on purpose, so only the persistence 
	 * stuff can get at it and not you. Stay away.
	 * 
	 * @return the clients (programs/machines) used with this account
	 */
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy="account")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	protected Set<Client> getClients() {
		return clients;
	}

	protected void setClients(Set<Client> clients) {
		if (clients == null)
			throw new IllegalArgumentException("null passed for clients");
		this.clients = clients;
	}

	/**
	 * The Person who owns this account. This is the 
	 * unique ID for the account.
	 * 
	 * @return Returns the owner.
	 */
	@OneToOne
	@JoinColumn(nullable=false)
	public User getOwner() {
		return owner;
	}

	/**
	 * This is protected because calling it is probably 
	 * a bad idea. (Give someone else your account?)
	 * 
	 * @param owner The owner to set.
	 */
	protected void setOwner(User owner) {
		this.owner = owner;
	}

	@Column(nullable=false)
	public Date getCreationDate() {
		if (creationDate < 0) {
			creationDate = System.currentTimeMillis();
		}
		return new Date(creationDate);
	}

	// only hibernate should call
	protected void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}
	
	@Column(nullable=true)
	public Date getLastLoginDate() {
		if (lastLoginDate < 0) {
			return null;
		}
		return new Date(lastLoginDate);
	}

	public void setLastLoginDate(Date date) {
		if (date != null)
			this.lastLoginDate = date.getTime();
		else
			this.lastLoginDate = -1;
	}	

	@Column(nullable=true)
	public Date getLastLogoutDate() {
		if (lastLogoutDate < 0) {
			return null;
		}
		return new Date(lastLogoutDate);
	}

	public void setLastLogoutDate(Date date) {
		if (date != null)
			this.lastLogoutDate = date.getTime();
		else
			this.lastLogoutDate = -1;
	}
	
	@Column(nullable=true)
	public Date getLastWebActivityDate() {
		if (lastWebActivityDate < 0) {
			return null;
		}
		return new Date(lastWebActivityDate);
	}

	public void setLastWebActivityDate(Date date) {
		if (date != null)
			this.lastWebActivityDate = date.getTime();
		else
			this.lastWebActivityDate = -1;
	}	

	@Column(nullable = false)
	public int getInvitations() {
		return invitations;
	}

	public void setInvitations(int invitations) {
		if (invitations < 0)
			throw new IllegalArgumentException("attempt to set negative invitation count: " + invitations);
		this.invitations = invitations;
	}
	
	public boolean canSendInvitations(int count) {
		return count <= invitations;
	}

	public void addInvitations(int count) {
		setInvitations(invitations + count);
	}
	
	public void deductInvitations(int count) {
		if (count > invitations)
			throw new IllegalStateException("attempt to deduct more invitations than exist " + count + " from " + invitations);
		
		setInvitations(invitations - count);
	}
	
	@Column(nullable=false)
	public boolean getHasDoneShareLinkTutorial() {
		return hasDoneShareLinkTutorial;
	}

	public void setHasDoneShareLinkTutorial(boolean hasDoneShareLinkTutorial) {
		this.hasDoneShareLinkTutorial = hasDoneShareLinkTutorial;
	}

	@Column(nullable=false)
	public boolean getWasSentShareLinkTutorial() {
		return wasSentShareLinkTutorial;
	}

	public void setWasSentShareLinkTutorial(boolean wasSentShareLinkTutorial) {
		this.wasSentShareLinkTutorial = wasSentShareLinkTutorial;
	}

	@Column(nullable=false)
	public boolean getHasAcceptedTerms() {
		return hasAcceptedTerms;
	}

	public void setHasAcceptedTerms(boolean hasAcceptedTerms) {
		this.hasAcceptedTerms = hasAcceptedTerms;
	}
	
	@Column(nullable=false)
	public boolean getNeedsDownload() {
		return needsDownload;
	}

	public void setNeedsDownload(boolean needsDownload) {
		this.needsDownload = needsDownload;
	}
	
	/**
	 * Get the salt for the user's password
	 * @return salt bytes as String, or null if none have been set
	 */
	public Integer getPasswordSalt() {
		return passwordSalt;
	}

	/**
	 * Set the salt for the user's password
	 * @param password String containing the password salt, or null to clear the salt
	 */
	public void setPasswordSalt(Integer passwordSalt) {
		this.passwordSalt = passwordSalt;
	}

	/**
	 * Get the hash of the Account's password. This IS nullable.
	 * 
	 * @return A String containing the hash of the password, or null to clear password
	 */
	protected String getPassword() {
		return password;
	}

	/**
	 * Set the Account's hashed password
	 * @param password A String with the hash of the password, or null to clear it
	 */
	public void setPassword(String password) {
		if (password != null) {
			password = password.trim(); // paranoia
		}
		this.password = password;
	}

	/**
	 * Set the Account's hash password given a plaintext password, including
	 * generation of salt bytes.
	 * @param password
	 */
	public void setPasswordPlainText(String password) {
		String passwordHash = null;
		if (password == null) {
			setPassword(null);
			setPasswordSalt(null);
		} else { 
			Random random = new Random();		
			Integer passwordSalt = random.nextInt();
			passwordHash = StringUtils.secureHash(password.trim(), passwordSalt);
			setPassword(passwordHash);
			setPasswordSalt(passwordSalt);
		}
	}
	
	/**
	 * Check a password attempt against the Account's stored password, if any
	 * @param attempt A String with the plain text password attempt
	 * @return A boolean true if the password matches
	 */
	public boolean checkPassword(String attempt) {
		String correctHash = getPassword();
		Integer passwordSalt = getPasswordSalt();
		if (correctHash == null)
			return false;
		if (passwordSalt == null)
			return false;
		if (attempt == null)
			return false;
		String attemptHash = StringUtils.secureHash(attempt.trim(), passwordSalt);
		return attemptHash.equals(correctHash);
	}
	
	@Transient
	public boolean getHasPassword() {
		return getPassword() != null;
	}
	
	@Column(nullable=false)
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	@Column(nullable=false)
	public boolean isAdminDisabled() {
		return adminDisabled;
	}

	public void setAdminDisabled(boolean adminDisabled) {
		this.adminDisabled = adminDisabled;
	}
	
	@Transient
	public boolean isActive() {
		return !(isDisabled() || isAdminDisabled()) && hasAcceptedTerms;
	}

	@Column(nullable=true)
	public Boolean isMusicSharingEnabled() {
		return musicSharingEnabled;
	}

	public void setMusicSharingEnabled(Boolean musicSharingEnabled) {
		this.musicSharingEnabled = musicSharingEnabled;
	}
	
	@Transient
	public boolean isMusicSharingEnabledWithDefault() {
		return musicSharingEnabled == null ? AccountSystem.DEFAULT_ENABLE_MUSIC_SHARING : musicSharingEnabled.booleanValue(); 
	}

	@Column(nullable=false)
	public boolean isMusicSharingPrimed() {
		return musicSharingPrimed;
	}

	public void setMusicSharingPrimed(boolean musicSharingPrimed) {
		this.musicSharingPrimed = musicSharingPrimed;
	}
	
	@Column(nullable=true)
	public Date getNativeMusicSharingTimestamp() {
		if (nativeMusicSharingTimestamp == -1)
			return null;
		return new Date(nativeMusicSharingTimestamp);
	}

	public void setNativeMusicSharingTimestamp(Date nativeMusicSharingTimestamp) {
		if (nativeMusicSharingTimestamp == null)
			this.nativeMusicSharingTimestamp = -1;
		else
			this.nativeMusicSharingTimestamp = nativeMusicSharingTimestamp.getTime();
	}

	@Column(nullable=true)
	public Boolean isNotifyPublicShares() {
		return notifyPublicShares;
	}

	public void setNotifyPublicShares(Boolean notify) {
		this.notifyPublicShares = notify;
	}
	
	/**
	 * Get the theme for the flash embed. Multiple users can be
	 * using the same theme, hence @ManyToOne.
	 * 
	 * @return name of theme or null for default
	 */
	@JoinColumn(nullable=true)
	@ManyToOne
	public NowPlayingTheme getNowPlayingTheme() {
		return nowPlayingTheme;
	}

	public void setNowPlayingTheme(NowPlayingTheme nowPlayingTheme) {
		this.nowPlayingTheme = nowPlayingTheme;
	}
	
	@Override
	@Transient
	public String getHumanReadableString() {
		return getOwner().getNickname();
	}
	
	@Override
	@Transient
	public String getDerivedNickname() {
		return getHumanReadableString();
	}
	
    // This is a @ManyToMany relationship because:
    // -- a user with a given account can have many favorite posts
	// -- the same post can have many users that marked it as a favorite post
	@ManyToMany
	@JoinTable(name="Account_Post",
		       joinColumns=@JoinColumn(name="Account_id", referencedColumnName="id"),                 
		       inverseJoinColumns=@JoinColumn(name="favoritePosts_id", referencedColumnName="id"),
		       uniqueConstraints=@UniqueConstraint(columnNames={"Account_id", "favoritePosts_id"}))	
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<Post> getFavoritePosts() {
		if (favoritePosts == null)
			throw new RuntimeException("no favorite posts?");
		return favoritePosts;
	}

	/**
	 * This is protected because only Hibernate probably 
	 * needs to call it. Use addFavoritePost/removeFavoritePost 
	 * instead...
	 * 
	 * @param favoritePosts the faves
	 */
	protected void setFavoritePosts(Set<Post> favoritePosts) {
		if (favoritePosts == null)
			throw new IllegalArgumentException("null fave posts");
		this.favoritePosts = favoritePosts;
	}
	
	public void addFavoritePost(Post post) {
		favoritePosts.add(post);
	}
	
	public void removeFavoritePost(Post post) {
		favoritePosts.remove(post);
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getMusicBio() {
		return musicBio;
	}

	public void setMusicBio(String musicBio) {
		this.musicBio = musicBio;
	}

	@Column(nullable=true)	
	public Date getLastSeenGroupInvitations() {
		if (lastSeenGroupInvitations >= 0)
			return new Date(lastSeenGroupInvitations);
		else
			return null;
	}

	// Only used by Hibernate
	protected void setLastSeenGroupInvitations(Date date) {
		if (date == null)
			this.lastSeenGroupInvitations = -1;
		else
			this.lastSeenGroupInvitations = date.getTime();
	}
	
	public void touchLastSeenGroupInvitations() {
		this.lastSeenGroupInvitations = System.currentTimeMillis();
	}

	// try LAZY fetch since for now we only need this on /account and /person
	// maybe it will make sense
	@OneToMany(mappedBy="account", fetch=FetchType.LAZY)
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<ExternalAccount> getExternalAccounts() {
		if (externalAccounts == null)
			throw new RuntimeException("no external accounts set???");
		return externalAccounts;
	}
	
	/**
	 * This is protected because only Hibernate probably 
	 * needs to call it.
	 * 
	 * @param externalAccounts
	 */
	protected void setExternalAccounts(Set<ExternalAccount> externalAccounts) {
		if (externalAccounts == null)
			throw new IllegalArgumentException("null external accounts");
		this.externalAccounts = externalAccounts;
	}	
	
	@Column(nullable=true)
	public Date getGroupInvitationReceived() {
		if (groupInvitationReceived >= 0)
			return new Date(groupInvitationReceived);
		else
			return null;
	}

	protected void setGroupInvitationReceived(Date date) {
		if (date == null)
			this.groupInvitationReceived = -1;
		else
			this.groupInvitationReceived = date.getTime();
	}

	public void touchGroupInvitationReceived() {
		this.groupInvitationReceived = System.currentTimeMillis();
	}
	
	@Transient
	private Set<ExternalAccount> getExternalAccountsBySentiment(Sentiment sentiment) {
		Set<ExternalAccount> filtered = new HashSet<ExternalAccount>();
		for (ExternalAccount a : getExternalAccounts()) {
			if (a.getSentiment() == Sentiment.LOVE) {
				filtered.add(a);
			}
		}
		return filtered;		
	}
	
	@Transient
	public Set<ExternalAccount> getLovedAccounts() {
		return getExternalAccountsBySentiment(Sentiment.LOVE);
	}
	
	@Transient
	public Set<ExternalAccount> getHatedAccounts() {
		return getExternalAccountsBySentiment(Sentiment.HATE);
	}
	
	@Transient
	public ExternalAccount getExternalAccount(ExternalAccountType type) {
		for (ExternalAccount a : getExternalAccounts()) {
			if (a.getAccountType() == type) {
				return a;
			}
		}
		return null;
	}

	@Column(nullable=true)
	public String getStackFilter() {
		return stackFilter;
	}

	public void setStackFilter(String blockTypeFilter) {
		this.stackFilter = blockTypeFilter;
	}
}
