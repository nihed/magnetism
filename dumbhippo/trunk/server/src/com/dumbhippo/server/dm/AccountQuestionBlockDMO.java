package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.server.blocks.AccountQuestionBlockView;
import com.dumbhippo.server.blocks.AccountQuestionButton;

@DMO(classId="http://mugshot.org/p/o/accountQuestionBlock")
public abstract class AccountQuestionBlockDMO extends BlockDMO {
	protected AccountQuestionBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@Override
	public String getTitle() {
		return ((AccountQuestionBlockView)blockView).getQuestion().getTitle();
	}
	
	@Override
	public String getDescription() {
		AccountQuestionBlockView questionBlockView = (AccountQuestionBlockView)blockView;
		
		return questionBlockView.getQuestion().getDescription(questionBlockView.getAnswer());
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getMoreLink() {
		return ((AccountQuestionBlockView)blockView).getQuestion().getMoreLink(((AccountQuestionBlockView)blockView).getLinkParam());
	}
	
	@DMProperty(defaultInclude=true)
	public String getAnswer() {
		return ((AccountQuestionBlockView)blockView).getAnswer();
	}
	
	@DMProperty(defaultInclude=true)
	public List<String> getButtons() {
		List<String> result = new ArrayList<String>();
		for (AccountQuestionButton button : ((AccountQuestionBlockView)blockView).getQuestion().getButtons()) {
			result.add(button.getResponse() + ":" + button.getText());
		}
		
		return result;
	}
}
