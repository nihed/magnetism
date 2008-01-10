package com.dumbhippo.test.util;

public abstract class Sequence {
	private Sequencer sequencer;
	int step = -1;
	
	public final void setSequencer(Sequencer sequencer) {
		this.sequencer = sequencer;
	}
	
	public final void setStep(int i) {
		synchronized(sequencer) {
			this.step = i;
		}
	}
	
	public final int getStep() {
		return step;
	}
	
	protected final void step(int i) {
		setStep(i);
		try {
			sequencer.waitForOthers(this);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting for other threads", e);
		}
	}
	
	abstract public void run() throws Exception;
}
