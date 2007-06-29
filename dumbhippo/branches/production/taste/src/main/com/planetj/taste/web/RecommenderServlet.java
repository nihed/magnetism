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

package com.planetj.taste.web;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.impl.model.ByValuePreferenceComparator;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Preference;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>A servlet which returns recommendations, as its name implies. The servlet accepts GET and POST
 * HTTP requests, and looks for two parameters:</p>
 *
 * <ul>
 *  <li><em>userID</em>: the user ID for which to produce recommendations</li>
 *  <li><em>howMany</em>: the number of recommendations to produce</li>
 *  <li><em>debug</em>: (optional) output a lot of information that is useful in debugging.
 *    Defaults to false, of course.</li>
 * </ul>
 *
 * <p>The response is text, and contains a list of the IDs of recommended items, in descending
 * order of relevance, one per line.</p>
 *
 * <p>For example, you can get 10 recommendations for user 123 from the following URL (assuming
 * you are running taste in a web application running locally on port 8080):<br/>
 * <code>http://localhost:8080/taste/RecommenderServlet?userID=123&amp;howMany=1</code></p>
 *
 * <p>This servlet requires one <code>init-param</code> in <code>web.xml</code>: it must find
 * a parameter named "recommender-class" which is the name of a class that implements
 * {@link Recommender} and has a no-arg constructor. The servlet will instantiate and use
 * this {@link Recommender} to produce recommendations.</p>
 *
 * @author Sean Owen
 */
public final class RecommenderServlet extends HttpServlet {

	private static final long serialVersionUID = 5146961602809970620L;

	private Recommender recommender;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		final String recommenderClassName = config.getInitParameter("recommender-class");
		if (recommenderClassName == null) {
			throw new ServletException("Servlet init-param \"recommender-class\" is not defined");
		}
		try {
			RecommenderSingleton.initializeIfNeeded(recommenderClassName);
		} catch (TasteException te) {
			throw new ServletException(te);
		}
		recommender = RecommenderSingleton.getInstance().getRecommender();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doGet(final HttpServletRequest request,
	                  final HttpServletResponse response) throws ServletException {

		final String userID = request.getParameter("userID");
		if (userID == null) {
			throw new ServletException("userID was not specified");
		}
		final String howManyString = request.getParameter("howMany");
		if (howManyString == null) {
			throw new ServletException("howMany was not specified");
		}
		final int howMany = Integer.parseInt(howManyString);
		final boolean debug = Boolean.valueOf(request.getParameter("debug"));

		try {

			final List<RecommendedItem> items = recommender.recommend(userID, howMany);

			response.setContentType("text/plain");
			final PrintWriter writer = response.getWriter();
			if (debug) {
				final DataModel dataModel = recommender.getDataModel();
				writer.println("User: " + dataModel.getUser(userID));
				writer.println();
				writer.println("Top 20 Preferences:");
				final Iterable<Preference> unsortedPrefs = dataModel.getUser(userID).getPreferences();
				final List<Preference> sortedPrefs = new ArrayList<Preference>();
				for (final Preference pref : unsortedPrefs) {
					sortedPrefs.add(pref);
				}
				Collections.sort(sortedPrefs, Collections.reverseOrder(new ByValuePreferenceComparator()));
				// Cap this at 20 just to be brief
				for (final Preference pref : sortedPrefs.subList(0, 20)) {
					writer.println(String.valueOf(pref.getValue()) + '\t' + pref.getItem());
				}
				writer.println();
				writer.println("Recommendations:");
				for (final RecommendedItem recommendedItem : items) {
					writer.println(String.valueOf(recommendedItem.getValue()) + '\t' +
					               recommendedItem.getItem());
				}
			} else {
				for (final RecommendedItem recommendedItem : items) {
					writer.println(String.valueOf(recommendedItem.getValue()) + '\t' +
					               recommendedItem.getItem().getID());
				}
			}

		} catch (TasteException te) {
			throw new ServletException(te);
		} catch (IOException ioe) {
			throw new ServletException(ioe);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doPost(final HttpServletRequest request,
	                   final HttpServletResponse response) throws ServletException {
		doGet(request, response);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		recommender = null;
		super.destroy();
	}

}
