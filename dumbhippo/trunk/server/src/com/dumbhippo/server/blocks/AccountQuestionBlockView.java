package com.dumbhippo.server.blocks;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

public class AccountQuestionBlockView extends BlockView implements TitleBlockView {
	private AccountQuestion question;
	String answer;
	String linkParam;
	
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
	
	public void populate(String answer, String linkParam) {
		this.answer = answer;
		this.linkParam = linkParam;
		
		setPopulated(true);
	}
	
	public AccountQuestion getQuestion() {
		return question;
	}
	
	@Override
	public String getIcon() {
		return "/images3/star.png";
	}

	public String getTitleForHome() {
		return getTitle();
	}
	public String getTitle() {
		return getQuestion().getTitle();
	}
	
	public String getLink() {
		return getQuestion().getTitleLink(linkParam);
	}
	
	public String getMoreLink() {
		return getQuestion().getMoreLink(linkParam);
	}
	
	public String getDescription() {
		return getQuestion().getDescription(answer);
	}
	
	public String getDescriptionAsHtml() {
		String description = getDescription();
		if (description.trim().length() > 0) {
			XmlBuilder xml = new XmlBuilder();
			xml.appendTextAsHtml(description, null);
			return xml.toString();
		} else {
			return "";
		}
	}
	
	@Override
	public String getBlockSummaryHeading() {
		return "New feature";
	}

	@Override
	public String getBlockSummaryLink() {
		if (getLink() != null)
			return getLink();
		
		return getQuestion().getMoreLink(linkParam);
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
							"moreLink", question.getMoreLink(linkParam));
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
	
	@Override
	public String getHomeUrl() {
		return "/person?who=" + getUserBlockData().getUser().getId();
	}

	public String getAnswer() {
		return answer;
	}
	
	public String getLinkParam() {
		return linkParam;
	}
	
	@Override
	public String getPrivacyTip() {
		return "Private: This update can only be seen by you.";
	}
}
