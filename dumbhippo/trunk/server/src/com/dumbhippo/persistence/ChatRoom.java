package com.dumbhippo.persistence;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Represents a chat room that exists now or existed at some point in time.
 * 
 * @author dff
 */

@Entity
public class ChatRoom extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Number of chat room messages to show in history
	 */
	// TODO: make configurable via config file
	private static final int NUM_MESSAGES_TO_SHOW = 3;
	
	// persistent
	private String name;
	private String type;
	private String botName;
	private long lastActivity;
	private List<ChatRoomMessage> messages;
	private Post post;
	private List<ChatRoomScreenName> roster;
	
	public ChatRoom() {
		this(null,null,null);
	}
	
	public ChatRoom(String name, Date lastActivity, Post post) {
		super();
		// hard coded since we only support AIM chat rooms at the moment
		this.type = "AIM";
		this.name = name;
		this.setLastActivity(lastActivity);
		this.post = post;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@OneToOne(optional=false)
	@JoinColumn(nullable=false)
	public Post getPost() {
		return post;
	}

	public void setPost(Post post) {
		this.post = post;
	}

	@Column(nullable=false)
	public Date getLastActivity() {
		return lastActivity >= 0 ? new Date(lastActivity) : null;
	}

	public void setLastActivity(Date lastActivity) {
		this.lastActivity = lastActivity != null ? lastActivity.getTime() : -1;
	}

	@OneToMany
	@JoinColumn(nullable=true)
	public List<ChatRoomMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<ChatRoomMessage> messages) {
		this.messages = messages;
	}
	
	@Column(nullable=false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBotName() {
		return botName;
	}

	public void setBotName(String botName) {
		this.botName = botName;
	}

	@OneToMany
	@JoinColumn(nullable=true)
	public List<ChatRoomScreenName> getRoster() {
		return roster;
	}

	public void setRoster(List<ChatRoomScreenName> roster) {
		this.roster = roster;
	}
	
	/** 
	 * Get a consistent name string appropriate for an AIM chat room using
	 * a hash of the guid hash associated with the GuidPersistable object.
	 * 
	 * @param baseString GuidPersistable object to base the name on
	 * @return Resulting chat room name
	 */
	public static String createChatRoomNameStringFor(GuidPersistable guid) {
		String guidHash = guid.hashCode()+"";
		String chatroom = "dh"+Math.abs(guidHash.hashCode());
		chatroom = chatroom.replaceAll("0","a");
		chatroom = chatroom.replaceAll("1","b");
		chatroom = chatroom.replaceAll("2","c");
		chatroom = chatroom.replaceAll("3","d");
		chatroom = chatroom.replaceAll("4","e");
		chatroom = chatroom.replaceAll("5","f");
		chatroom = chatroom.replaceAll("6","g");
		chatroom = chatroom.replaceAll("7","h");
		chatroom = chatroom.replaceAll("8","i");
		chatroom = chatroom.replaceAll("9","j");
		return chatroom;
	}
	
	@Transient
	public static int getPreviewMessageCount() {
		return NUM_MESSAGES_TO_SHOW;
	}
}