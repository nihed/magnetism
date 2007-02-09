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

package com.planetj.taste;

import com.planetj.taste.common.CommonTest;
import com.planetj.taste.impl.LoadTest;
import com.planetj.taste.impl.common.EmptyIteratorTest;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrerTest;
import com.planetj.taste.impl.correlation.CosineMeasureCorrelationTest;
import com.planetj.taste.impl.correlation.PearsonCorrelationTest;
import com.planetj.taste.impl.correlation.SpearmanCorrelationTest;
import com.planetj.taste.impl.model.file.FileDataModelTest;
import com.planetj.taste.impl.model.jdbc.MySQLJDBCDataModelTest;
import com.planetj.taste.impl.neighborhood.NearestNNeighborhoodTest;
import com.planetj.taste.impl.neighborhood.ThresholdNeighborhoodTest;
import com.planetj.taste.impl.recommender.CachingRecommenderTest;
import com.planetj.taste.impl.recommender.GenericItemBasedRecommenderTest;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommenderTest;
import com.planetj.taste.impl.transforms.CaseAmplificationTest;
import com.planetj.taste.impl.transforms.InverseUserFrequencyTest;
import com.planetj.taste.impl.transforms.ZScoreTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * <p>Runs all tests.</p>
 *
 * @author Sean Owen
 */
public final class AllTests extends TestCase {

	public static Test suite() {
		final TestSuite suite = new TestSuite();
		suite.addTestSuite(CommonTest.class);
		suite.addTestSuite(EmptyIteratorTest.class);
		suite.addTestSuite(AveragingPreferenceInferrerTest.class);
		suite.addTestSuite(CosineMeasureCorrelationTest.class);
		suite.addTestSuite(PearsonCorrelationTest.class);
		suite.addTestSuite(SpearmanCorrelationTest.class);
		suite.addTestSuite(NearestNNeighborhoodTest.class);
		suite.addTestSuite(ThresholdNeighborhoodTest.class);
		suite.addTestSuite(CaseAmplificationTest.class);
		suite.addTestSuite(InverseUserFrequencyTest.class);
		suite.addTestSuite(ZScoreTest.class);
		suite.addTestSuite(FileDataModelTest.class);
		suite.addTestSuite(MySQLJDBCDataModelTest.class);
		suite.addTestSuite(GenericItemBasedRecommenderTest.class);
		suite.addTestSuite(GenericUserBasedRecommenderTest.class);
		suite.addTestSuite(CachingRecommenderTest.class);
		suite.addTestSuite(LoadTest.class);
		return suite;
	}

	public static void main(final String... args) {
		TestRunner.run(suite());
	}

}
