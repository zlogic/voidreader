/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.feed;

import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zlogic.voidreader.Settings;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;
import org.zlogic.voidreader.handler.impl.EmailHandler;

/**
 * Toplevel class for handling all feeds: saving data to XML, restoring data
 * from XML, loading feeds list from OPML, downloading & handling new feed
 * items.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class FeedsState {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * The logger
	 */
	private static final Logger log = LoggerFactory.getLogger(FeedsState.class);
	/**
	 * The DatastoreService instance
	 */
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	/**
	 * Error handler
	 */
	private final ErrorHandler errorHandler;
	/**
	 * Feed item handler
	 */
	private final FeedItemHandler feedItemHandler;
	/**
	 * The date after which feed items expire and can be removed
	 */
	private final Date cacheExpiryDate;
	/**
	 * The user settings
	 */
	private final Settings settings;

	/**
	 * Constructor for FeedsState
	 *
	 * @param settings the user settings
	 */
	public FeedsState(Settings settings) {
		this.settings = settings;
		EmailHandler emailHandler = new EmailHandler(settings);
		errorHandler = emailHandler;
		feedItemHandler = emailHandler;

		Calendar expiryDate = new GregorianCalendar();
		expiryDate.add(Calendar.DAY_OF_MONTH, -settings.getCacheExpireDays());
		cacheExpiryDate = expiryDate.getTime();
	}

	/**
	 * Updates feed data by adding/removing items to match it with the OPML data
	 *
	 * @param opml the ROME OPML data
	 */
	public void updateOpml(Opml opml) {
		List<Feed> newFeeds = loadFeeds(opml.getOutlines(), null);
		List<Feed> existingFeeds = getFeeds();
		//Remove items absent from OPML
		List<Key> removeItems = new LinkedList<>();
		for (Feed feed : existingFeeds) {
			if (!newFeeds.contains(feed)) {
				removeItems.add(feed.getKey());
				for (FeedItem item : feed.getItems())
					removeItems.add(item.getKey());
			}
		}
		datastore.delete(removeItems);
		//Update/add items from OPML
		for (Feed feed : newFeeds) {
			try {
				feed.useItemsFrom(Feed.load(feed.getKey(), settings));
			} catch (EntityNotFoundException ex) {
			}
			feed.save();
		}
	}

	/**
	 * Recursively parses ROME OPML feed outlines
	 *
	 * @param outlines the feed outlines
	 * @param parentTitles the list of parent feed titles, or null if this is
	 * the top level
	 * @return the list of Feed instances
	 */
	private List<Feed> loadFeeds(List<Outline> outlines, List<String> parentTitles) {
		List<Feed> loadedFeeds = new LinkedList<>();
		if (parentTitles == null)
			parentTitles = new LinkedList<>();
		for (Outline obj : outlines) {
			Outline outline = (Outline) obj;
			List<String> currentParentTitles = new LinkedList<>(parentTitles);
			currentParentTitles.add(outline.getTitle());
			if ("rss".equals(outline.getType()) && outline.getXmlUrl() != null) //NOI18N
				loadedFeeds.add(new Feed(outline.getXmlUrl(), currentParentTitles, settings));
			loadedFeeds.addAll(loadFeeds(outline.getChildren(), currentParentTitles));
		}
		return loadedFeeds;
	}

	/**
	 * Returns the list of feeds
	 *
	 * @return the list of feeds
	 */
	public List<Feed> getFeeds() {
		return Feed.load(settings);
	}

	/**
	 * Downloads the latest feed data and handles new and updated items
	 *
	 * @throws TimeoutException if the task took too long to complete
	 */
	public void update() throws TimeoutException {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), ThreadManager.currentRequestThreadFactory());
		for (Feed feed : getFeeds()) {
			executor.submit(new Runnable() {
				private Feed feed;

				public Runnable setParameters(Feed feed) {
					this.feed = feed;
					return this;
				}

				@Override
				public void run() {
					try {
						feed.update(feedItemHandler, cacheExpiryDate, settings.getMaxRunSeconds());
						feed.save();
					} catch (Exception ex) {
						log.error(messages.getString("ERROR_OCCURRED_WHILE_UPDATING_FEED"), ex);//TODO: use an error handler
					}
				}
			}.setParameters(feed));
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(settings.getMaxRunSeconds() > 0 ? settings.getMaxRunSeconds() : Long.MAX_VALUE, TimeUnit.SECONDS)) {
				throw new TimeoutException(messages.getString("TIMED_OUT_WAITING_FOR_EXECUTOR"));
			}
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
