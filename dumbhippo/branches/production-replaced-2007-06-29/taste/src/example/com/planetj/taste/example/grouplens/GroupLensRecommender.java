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

package com.planetj.taste.example.grouplens;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.impl.transforms.CaseAmplification;
import com.planetj.taste.impl.transforms.InverseUserFrequency;
import com.planetj.taste.impl.transforms.ZScore;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;
import com.planetj.taste.recommender.ItemFilter;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

/**
 * A simple {@link Recommender} implemented for the GroupLens demo.
 *
 * @author Sean Owen
 */
public final class GroupLensRecommender implements Recommender {

	private final Recommender recommender;

	public GroupLensRecommender() throws IOException, TasteException {
		final File ratingsFile = readResourceToGLTempFile("/com/planetj/taste/example/grouplens/ratings.dat", true);
		final File moviesFile = readResourceToGLTempFile("/com/planetj/taste/example/grouplens/movies.dat", false);
		final DataModel dataModel = new GroupLensDataModel(ratingsFile, moviesFile);
		dataModel.addTransform(new ZScore());
		dataModel.addTransform(new CaseAmplification(1.5));
		dataModel.addTransform(new InverseUserFrequency(dataModel));
		recommender = new CachingRecommender(
			new GenericUserBasedRecommender(
				dataModel, new NearestNUserNeighborhood(10, new PearsonCorrelation(dataModel), dataModel)));
	}

	@NotNull
	public List<RecommendedItem> recommend(final Object userID, final int howMany) throws TasteException {
		return recommender.recommend(userID, howMany);
	}

	@NotNull
	public List<RecommendedItem> recommend(final Object userID, final int howMany, final ItemFilter filter)
		throws TasteException {
		return recommender.recommend(userID, howMany, filter);
	}

	public double estimatePreference(final Object userID, final Object itemID) throws TasteException {
		return recommender.estimatePreference(userID, itemID);
	}

	public void setPreference(final Object userID, final Object itemID, final double value) throws TasteException {
		recommender.setPreference(userID, itemID, value);
	}

	@NotNull
	public DataModel getDataModel() {
		return recommender.getDataModel();
	}

	public void refresh() {
		recommender.refresh();
	}

	@NotNull
	private static File readResourceToGLTempFile(final String resourceName,
	                                             final boolean ratingsFileHack) throws IOException {
		// Now translate the file; remove commas, then convert "::" delimiter to comma
		final File resultFile = File.createTempFile("taste", "txt");
		resultFile.deleteOnExit();
		final BufferedReader reader = new BufferedReader(
			new InputStreamReader(GroupLensRecommender.class.getResourceAsStream(resourceName)));
		final PrintWriter writer = new PrintWriter(new FileWriter(resultFile));
		for (String line; ((line = reader.readLine()) != null); ) {
			if (ratingsFileHack) {
				// Hack: toss the last column of data, which is a timestamp we don't want
				line = line.substring(0, line.lastIndexOf("::"));
			}
			line = line.replace(",", "").replace("::", ",");
			writer.println(line);
		}
		writer.close();
		reader.close();
		return resultFile;
	}

}
