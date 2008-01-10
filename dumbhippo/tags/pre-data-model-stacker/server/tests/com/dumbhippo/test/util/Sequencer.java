package com.dumbhippo.test.util;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class Sequencer {
	private static Logger logger = GlobalSetup.getLogger(Sequencer.class);
	
	private Sequence[] sequences; 

	public synchronized void waitForOthers(Sequence sequence) throws InterruptedException {
		int until = sequence.getStep();
		boolean ready = false;
		int index = 0;
		
		// Wake up any previously waiting threads since we have a new sequence number
		notifyAll();
		
		for (int i = 0; i < sequences.length; i++) {
			if (sequences[i] == sequence)
				index = i + 1;
		}
		
		while (true) {
			ready = true;
			for (int i = 0; i < sequences.length; i++) {
				if (sequences[i].getStep() < until) {
					ready = false;
				}
			}
			
			if (ready)
				break;
			
			wait();
		}

		logger.info("sequence-{}: Starting step {}", index, until);
	}
	
	public Sequencer(Sequence[] sequences) {
		this.sequences = sequences;
	}
	
	public void run() {
		SequenceThread[] threads = new SequenceThread[sequences.length];
		for (int i = 0; i < sequences.length; i++) {
			sequences[i].setSequencer(this);
		}

		for (int i = 0; i < sequences.length; i++) {
			threads[i] = new SequenceThread(i + 1, sequences[i]);
			threads[i].start();
		}
		
		for (int i = 0; i < sequences.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for sequence thread to finish");
			}
			Exception e = threads[i].getException();
			if (e != null) {
				throw new RuntimeException("sequence-" + (i + 1) + " threw exception", e);
			}
		}
	}
	
	public static void run2(Sequence sequence1, Sequence sequence2) {
		Sequencer sequencer = new Sequencer(new Sequence[] { sequence1, sequence2 });
		sequencer.run();
	}

	public class SequenceThread extends Thread {
		private Sequence sequence;
		private Exception exception;

		public SequenceThread(int index, Sequence sequence) {
			super("sequence-" + index);
			this.sequence = sequence;
		}
		
		@Override
		public void run() {
			try {
				sequence.run();
			} catch (Exception e) {
				logger.debug("{}: Caught exception: {}", this.getName(), e.getMessage());
				this.exception = e;
			}

			synchronized(Sequencer.this) {
				sequence.setStep(Integer.MAX_VALUE);
				Sequencer.this.notifyAll();
			}
		}
		
		public Exception getException() {
			return exception;
		}
	}
}
