/*
 * Copyright 2005 Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planetj.taste.impl;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericItemBasedRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.impl.transforms.CaseAmplification;
import com.planetj.taste.impl.transforms.InverseUserFrequency;
import com.planetj.taste.impl.transforms.ZScore;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * <p>Generates load on the whole implementation, for profiling purposes mostly.</p>
 *
 * @author Sean Owen
 */
public final class LoadTest extends TasteTestCase {

	private static final int NUM_USERS = 200;
	private static final int NUM_ITEMS = 400;
	private static final int NUM_PREFS = 15;
	private static final int NUM_THREADS = 4;

	private final Random random =
		new SecureRandom(new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF});

	@Override
	public void setUp() throws Exception {
		super.setUp();
		setLogLevel(Level.INFO);
	}

	public void testItemLoad() throws Exception {

		final DataModel model = createModel();

		final ItemCorrelation itemCorrelation = new PearsonCorrelation(model);
		final Recommender recommender = new CachingRecommender(new GenericItemBasedRecommender(model, itemCorrelation));

		doTestLoad(recommender);
	}

	public void testUserLoad() throws Exception {

		final DataModel model = createModel();

		final UserCorrelation userCorrelation = new PearsonCorrelation(model);
		userCorrelation.setPreferenceInferrer(new AveragingPreferenceInferrer());
		final UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, userCorrelation, model);
		final Recommender recommender = new CachingRecommender(new GenericUserBasedRecommender(model, neighborhood));

		doTestLoad(recommender);
	}

	private DataModel createModel() {

		final List<Item> items = new ArrayList<Item>(NUM_ITEMS);
		for (int i = 0; i < NUM_ITEMS; i++) {
			items.add(new GenericItem<String>(String.valueOf(i)));
		}

		final List<User> users = new ArrayList<User>(NUM_USERS);
		for (int i = 0; i < NUM_USERS; i++) {
			final int numPrefs = random.nextInt(NUM_PREFS) + 1;
			final List<Preference> prefs = new ArrayList<Preference>(numPrefs);
			for (int j = 0; j < numPrefs; j++) {
				prefs.add(new GenericPreference(null, items.get(random.nextInt(NUM_ITEMS)), random.nextDouble()));
			}
			final GenericUser<String> user = new GenericUser<String>(String.valueOf(i), prefs);
			users.add(user);
		}

		final GenericDataModel model = new GenericDataModel(users);
		model.addTransform(new ZScore());
		model.addTransform(new CaseAmplification(1.5));
		model.addTransform(new InverseUserFrequency(model));
		return model;
	}

	private void doTestLoad(final Recommender recommender)
		throws InterruptedException, ExecutionException {

		final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
		final List<Future<?>> futures = new ArrayList<Future<?>>(NUM_THREADS);
		final Callable<?> loadTask =
			new Callable<Object>() {
				public Object call() throws TasteException {
					for (int i = 0; i < NUM_USERS; i++) {
						recommender.recommend(String.valueOf(random.nextInt(NUM_USERS)), 10);
						if (i % 100 == 50) {
							recommender.refresh();
						}
					}
					return Boolean.TRUE;
				}
			};

		final long start = System.currentTimeMillis();
		for (int i = 0; i < NUM_THREADS; i++) {
			futures.add(executor.submit(loadTask));
		}
		for (final Future<?> future : futures) {
			future.get();
		}
		final long end = System.currentTimeMillis();

		assertTrue(end - start < 60000);
	}

}
