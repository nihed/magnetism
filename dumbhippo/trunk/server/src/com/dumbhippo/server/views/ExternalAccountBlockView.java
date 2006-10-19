package com.dumbhippo.server.views;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.UserBlockData;

public abstract class ExternalAccountBlockView extends BlockView {
	
	public ExternalAccountBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}

	public abstract ExternalAccountType getAccountType();
}
