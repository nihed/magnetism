package com.dumbhippo.server.views;

import java.util.List;

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
