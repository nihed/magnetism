package com.dumbhippo.server.views;

import java.util.List;

import com.dumbhippo.server.blocks.BlockView;

public class PersonMugshotView {

	private PersonView personView;
	private List<BlockView> blocks;
	
	public PersonMugshotView(PersonView personView, List<BlockView> blocks) {
		this.personView = personView;
		this.blocks = blocks;
	}

	public PersonView getPersonView() {
		return personView;
	}
	
	public List<BlockView> getBlocks() {
		return blocks;
	}
}
