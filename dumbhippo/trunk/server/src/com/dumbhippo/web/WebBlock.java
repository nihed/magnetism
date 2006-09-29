package com.dumbhippo.web;

import com.dumbhippo.persistence.Block;

public class WebBlock {
	private Block block;

	public WebBlock(Block block) {
		this.block = block;
	}

	public Block getBlock() {
		return block;
	}

	public String getWebTitle() {
		switch (block.getBlockType()) {
		case POST:
			return "Web Swarm";
		case GROUP_MEMBER:
			return "Mugshot";
		case MUSIC_PERSON:
			return "Music Radar";
		case EXTERNAL_ACCOUNT_UPDATE:
		case EXTERNAL_ACCOUNT_UPDATE_SELF:
			// TODO: needs to get names
			return getBlock().getBlockType().toString();
		}
		throw new RuntimeException("Unknown block type of block " + block);
	}

	public String getIconName() {
		switch (block.getBlockType()) {
		case POST:
			return "webswarm_icon.png";
		case GROUP_CHAT:
		case GROUP_MEMBER:
			return "mugshot_icon.png";
		case MUSIC_PERSON:
			return "musicradar_icon.png";
		case EXTERNAL_ACCOUNT_UPDATE:
		case EXTERNAL_ACCOUNT_UPDATE_SELF:
			// TODO: needs to retrieve favicons
			return "quiplove_icon.png";
		}
		throw new RuntimeException("Unknown block type of block " + block);
	}
}
