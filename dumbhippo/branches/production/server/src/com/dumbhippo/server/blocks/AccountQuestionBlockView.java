package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class AccountQuestionBlockView extends BlockView {
	private AccountQuestion question;
	String answer;
	
	public AccountQuestionBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
		
		for (AccountQuestion q : AccountQuestion.values()) {
			if (q.ordinal() == block.getData3()) {
				question = q;
				break;
			}
		}
		
		if (question == null) {
			throw new RuntimeException("Unknown question value " + block.getData3());
		}
	}
	
	public void populate(String answer) {
		this.answer = answer;
		
		setPopulated(true);
	}
	
	public AccountQuestion getQuestion() {
		return question;
	}
	
	@Override
	public String getIcon() {
		return "/images3/star.png";
	}

	@Override
	public String getSummaryHeading() {
		return "New feature";
	}

	@Override
	public String getSummaryLink() {
		return getQuestion().getMoreLink();
	}

	@Override
	public String getSummaryLinkText() {
		return getQuestion().getTitle();
	}

	@Override
	public String getTypeTitle() {
		return "New feature";
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("accountQuestion",
							"answer", answer,
							"moreLink", question.getMoreLink());
		builder.appendTextNode("title", question.getTitle());
		builder.appendTextNode("description", question.getDescription(answer));
		
		if (answer == null) {
			builder.openElement("buttons");
			for (AccountQuestionButton button : question.getButtons()) {
				builder.openElement("button", "response", button.getResponse());
				builder.appendTextNode("text", button.getText());
				builder.closeElement();
			}
			builder.closeElement();
		}
		
		builder.closeElement();
	}

	public List<Object> getReferencedObjects() {
		return Collections.emptyList();
	}
}
