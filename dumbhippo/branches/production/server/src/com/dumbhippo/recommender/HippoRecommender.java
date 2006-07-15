package com.dumbhippo.recommender;

import java.io.IOException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import javax.sql.DataSource;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.model.jdbc.AbstractJDBCDataModel;
import com.planetj.taste.impl.model.jdbc.MySQLJDBCDataModel;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

/**
 * A simple {@link Recommender} using the Taste recommender engine.
 *
 * @author dff
 */
public final class HippoRecommender implements Recommender {
	
	private Recommender recommender;
	
	// size of neighborhood to calculate for Nearest-N Neighborhood
	// TODO: probably bump this up once we have more users
	private static int NEIGHBORHOOD_SIZE = 5;

	public HippoRecommender(DataSource dataSource) throws IOException, TasteException {	

		// force some SQL debug information out of Taste's JDBC data model
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
	    java.util.logging.Logger other_logger = 
	    	java.util.logging.Logger.getLogger(AbstractJDBCDataModel.class.getName());
	    other_logger.addHandler(handler);
	    other_logger.setLevel(Level.ALL);
	    
	    // force debug out of Taste user-based recommender
	    other_logger = java.util.logging.Logger.getLogger(GenericUserBasedRecommender.class.getName());
	    other_logger.addHandler(handler);
	    other_logger.setLevel(Level.ALL);
	    
		DataModel dataModel = new MySQLJDBCDataModel(dataSource, "Rating", "user_id", "item_id", "score");
		//dataModel.addTransform(new ZScore());
		//dataModel.addTransform(new CaseAmplification(1.5));
		//dataModel.addTransform(new InverseUserFrequency(dataModel));
		
		// use a user-based recommender
		// TODO: try an item-based recommender, should be more performant since correlations can be cached
		
		UserCorrelation userCorrelation = new PearsonCorrelation(dataModel);
		userCorrelation.setPreferenceInferrer(new AveragingPreferenceInferrer());
		UserNeighborhood neighborhood = new NearestNUserNeighborhood(NEIGHBORHOOD_SIZE, userCorrelation, dataModel);
		recommender = new GenericUserBasedRecommender(dataModel, neighborhood);
		//recommender = new CachingRecommender(userRecommender);		
	}

	public List<RecommendedItem> recommend(final Object userID, final int howMany) throws TasteException {
		return recommender.recommend(userID, howMany);
	}

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
	
	public DataModel getDataModel() {
		return recommender.getDataModel();
	}

	public void refresh() {
		recommender.refresh();
	}
}
