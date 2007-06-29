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

import com.planetj.taste.impl.model.file.FileDataModel;
import com.planetj.taste.model.Item;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sean Owen
 */
final class GroupLensDataModel extends FileDataModel {

	private final Map<String, String> movieTitleMap;
	private final Map<String, String> genreMap;

	GroupLensDataModel(final File ratingsFile, final File moviesFile) throws IOException {
		super(ratingsFile);
		movieTitleMap = new HashMap<String, String>();
		genreMap = new HashMap<String, String>();
		final BufferedReader reader = new BufferedReader(new FileReader(moviesFile));
		try {
			for (String line; (line = reader.readLine()) != null; ) {
				final String[] tokens = line.split(",");
				movieTitleMap.put(tokens[0], tokens[1]);
				genreMap.put(tokens[0], tokens[2]);
			}
		} finally {
			reader.close();
		}
	}

	@Override
	protected Item buildItem(final String id) {
		final String movieTitle = movieTitleMap.get(id);
		final String genres = genreMap.get(id);
		return new Movie(id, movieTitle, genres);
	}

}
