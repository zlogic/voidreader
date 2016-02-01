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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zlogic.voidreader.Settings;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

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
	 * @param feedItemHandler the FeedItemHandler instance
	 * @param errorHandler the ErrorHandler instance
	 */
	public FeedsState(Settings settings, FeedItemHandler feedItemHandler, ErrorHandler errorHandler) {
		this.settings = settings;
		this.feedItemHandler = feedItemHandler;
		this.errorHandler = errorHandler;

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
		List<Key> removeItems = new ArrayList<>();
		for (Feed feed : existingFeeds)
			if (!newFeeds.contains(feed))
				removeItems.add(feed.getKey());
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
		List<Feed> loadedFeeds = new ArrayList<>();
		if (parentTitles == null)
			parentTitles = new ArrayList<>();
		for (Outline obj : outlines) {
			Outline outline = (Outline) obj;
			List<String> currentParentTitles = new ArrayList<>(parentTitles);
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
	 * @throws InterruptedException if the task was interrupted
	 * @throws ExecutionException if the task threw an exception
	 */
	public void update() throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool(settings.getThreadPoolSize(), ThreadManager.currentRequestThreadFactory());
		List<Feed> feeds = getFeeds();
		List<Future> futures = new ArrayList<>();
		for (Feed feed : feeds) {
			Future<?> future = executor.submit(new Runnable() {
				private Feed feed;
				private ExecutorService executor;

				public Runnable setParameters(Feed feed, ExecutorService executor) {
					this.feed = feed;
					this.executor = executor;
					return this;
				}

				@Override
				public void run() {
					try {
						feed.update(feedItemHandler, cacheExpiryDate, executor);
						feed.save();
					} catch (Throwable thr) {
						log.error(messages.getString("ERROR_OCCURRED_WHILE_UPDATING_FEED"), thr);//TODO: use an error handler
					}
				}
			}.setParameters(feed, executor));
			futures.add(future);
		}
		for (Future<?> f : futures)
			f.get();
		List<Runnable> uncompletedTasks = executor.shutdownNow();
		if (uncompletedTasks.isEmpty())
			log.info(messages.getString("ALL_TASKS_FINISHED"));
		else
			log.error(messages.getString("DIDNT_COMPLETE_TASKS"), uncompletedTasks.size());
	}
}
