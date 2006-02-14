package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class LivePostBoardBean implements LivePostBoard {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LivePostBoardBean.class);
	
	@EJB
	IdentitySpider identitySpider;
	
	@EJB
	PostingBoard postingBoard;
	
	@PersistenceContext(unitName = "dumbhippo")
	EntityManager em;

	public String getLivePostXML(Viewpoint viewpoint, LivePost livePost) {
		XmlBuilder builder = new XmlBuilder();
		PostView pv;		
		try {
			pv = postingBoard.loadPost(viewpoint, livePost.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		
		Post post = pv.getPost();
		builder.appendTextNode("id", post.getId());
		builder.appendTextNode("senderName", pv.getPoster().getName());		
		builder.appendTextNode("title", post.getTitle());
		builder.appendTextNode("chattingUserCount", Integer.toString(livePost.getChattingUserCount()));
		builder.appendTextNode("viewingUserCount", Integer.toString(livePost.getChattingUserCount()));
		
		return builder.toString();
	}
}
