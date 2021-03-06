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

import com.planetj.taste.impl.model.GenericItem;

import org.jetbrains.annotations.NotNull;

/**
 * @author Sean Owen
 */
final class Movie extends GenericItem<String> {

	private static final long serialVersionUID = -4229758934836722235L;

	private final String movieTitle;
	private final String genres;

	Movie(final String id, final String movieTitle, final String genres) {
		super(id);
		this.movieTitle = movieTitle;
		this.genres = genres;
	}

	@Override
	@NotNull
	public String toString() {
		return getID().toString() + '\t' + movieTitle + '\t' + genres;
	}

}
