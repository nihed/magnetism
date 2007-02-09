package com.dumbhippo.hungry.performance;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.dumbhippo.hungry.util.SkipTest;

import junit.framework.TestCase;

/**
 * An abstract class that does performance measurement for a 
 * single page. The subclass sets the particular page to measure
 * in its constructor.
 * 
 * @author otaylor
 */
@SkipTest
public class TimePage extends TestCase {
	private String url;
	
	protected TimePage(String url) {
		this.url = url;
	}
	
	@Override
	public void setUp() {
	}
	
	protected void timePage(int nThreads, final int nIterations) {
		if (nThreads == 1 && nIterations == 1) {
			System.out.println("\nFetching " + url + " (see http://devel.dumbhippo.com/wiki/RunningHungry#Performance)");
			System.out.println(" Threads   Iters    Wall Iters/s  Thread Iters/s Latency");
			System.out.println(" ------- ------- ------- ------- ------- ------- -------");
		}

		Slurper[] slurpers = new Slurper[nThreads];
		
		for (int i = 0; i < nThreads; i++) {
			slurpers[i] = new Slurper("hippo" + (i + 1) + "@example.com");
		}

		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		@SuppressWarnings("unchecked")
		Future<Long>[] futures = new Future[nThreads];
		
		long start = System.currentTimeMillis();
		
		// Fire the requests to fetch the pages off to our thread pool
		for (int i = 0; i < nThreads; i++) {
			final Slurper slurper = slurpers[i]; 
			futures[i] = executor.submit(new Callable<Long>() {
				public Long call() {
					long start = System.currentTimeMillis();
					
					for (int j = 0; j < nIterations; j++) {
						slurper.slurpUrl(url);
					}
					
					long now = System.currentTimeMillis();
					
					return now - start; 
				}
			});
		}

		// Collect the results
		long totalThreadTime = 0;
		for (int i = 0; i < nThreads; i++) {
			long time;
			
			try {
				time = futures[i].get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Error getting result of slurper thread", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Error getting result of slurper thread", e);
			}
			
			totalThreadTime += time;
		}
		
		long now = System.currentTimeMillis();
		
		double wallTime = (now - start) / 1000.;
		double threadTime = (totalThreadTime) / (1000. * nThreads);
		
		System.out.println(String.format("%8d%8d%8.3f%8.3f%8.3f%8.3f%8.3f",
				           nThreads, nIterations,
				           wallTime, 
				           (nIterations * nThreads) / wallTime, 
				           threadTime, 
				           (nIterations * nThreads) / threadTime,
				           threadTime / nIterations));
	}
}
