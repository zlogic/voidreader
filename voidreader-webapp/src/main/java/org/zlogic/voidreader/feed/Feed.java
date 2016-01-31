/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.feed;

import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zlogic.voidreader.Settings;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 * A single RSS feed representation (with a cached list of feed items). Contains
 * annotations used by JAXB marshaling/unmarshaling and can be persisted in XML
 * form.
 *
 * Used to keep track of already downloaded/sent items.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class Feed {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * The logger
	 */
	private static final Logger log = LoggerFactory.getLogger(Feed.class);
	/**
	 * The DatastoreService instance
	 */
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	/**
	 * The feed URL
	 */
	private String url;
	/**
	 * The feed items
	 */
	private Set<FeedItem> items;
	/**
	 * The feed title, as presented in the RSS download results
	 */
	private String title;
	/**
	 * The feed encoding
	 */
	private String encoding;
	/**
	 * The feed title, as presented in the OPML file
	 */
	private List<String> userTitle;
	/**
	 * The user settings
	 */
	private final Settings settings;

	/**
	 * Empty constructor
	 */
	private Feed() {
		settings = null;
	}

	/**
	 * Creates the feed from data extracted from OPML
	 *
	 * @param url the feed URL
	 * @param userTitle the feed title as presented in the OPML
	 * @param settings the user Settings
	 */
	protected Feed(String url, List<String> userTitle, Settings settings) {
		this.url = url;
		this.userTitle = new LinkedList<>(userTitle);
		this.items = new HashSet<>();
		this.settings = settings;
	}

	/**
	 *
	 * Constructs a Feed instance from a Datastore Entity.
	 *
	 * @param entity the Datastore Entity
	 * @param settings the user Settings
	 */
	private Feed(Entity entity, Settings settings) {
		this.url = entity.getKey().getName();
		this.settings = settings;
	}

	/**
	 * Loads all Feed instances for user from Datastore.
	 *
	 * @param settings the user Settings
	 * @return the list of all Feed instances for user from Datastore
	 */
	public static List<Feed> load(Settings settings) {
		List<Feed> feeds = new LinkedList<>();
		Query query = new Query(Feed.class.getSimpleName(), settings.getKey());
		PreparedQuery preparedQuery = datastore.prepare(query);
		for (Entity result : preparedQuery.asIterable()) {
			Feed feed = new Feed(result, settings);
			feed.items = feed.loadItems();
			feeds.add(feed);
		}
		return feeds;
	}

	/**
	 * Loads a Feed instance for user from Datastore.
	 *
	 * @param key the feed key
	 * @param settings the user Settings
	 * @return the feed
	 * @throws EntityNotFoundException if not Feed instance for key is found
	 */
	public static Feed load(Key key, Settings settings) throws EntityNotFoundException {
		return new Feed(datastore.get(key), settings);
	}

	/**
	 * Loads all FeedItem instances for Feed from Datastore.
	 *
	 * @return the list of all FeedItem instances for Feed from Datastore
	 */
	private Set<FeedItem> loadItems() {
		Set<FeedItem> feedItems = new HashSet<>();
		Query query = new Query(FeedItem.class.getSimpleName(), getKey());
		PreparedQuery preparedQuery = datastore.prepare(query);
		for (Entity result : preparedQuery.asIterable())
			feedItems.add(new FeedItem(this, result));
		return feedItems;
	}

	/**
	 * Sets feed items from another Feed.
	 *
	 * @param feed the feed from which to copy feed items
	 */
	public void useItemsFrom(Feed feed) {
		items = feed.items;
	}

	/**
	 * Saves this Feed instance to Datastore. Doesn't process FeedItems.
	 */
	public void save() {
		Entity feed = new Entity(getKey());
		datastore.put(feed);
	}

	/**
	 * Returns the key for a Feed based on its URL.
	 *
	 * @param settings the user Settings
	 * @param url the Feed URL
	 * @return the key for a Feed based on its URL
	 */
	public static Key getKey(Settings settings, String url) {
		return new Entity(Feed.class.getSimpleName(), url, settings.getKey()).getKey();
	}

	/**
	 * Returns the key for this Feed instance.
	 *
	 * @return the key for this Feed instance
	 */
	public Key getKey() {
		return getKey(settings, url);
	}

	/**
	 * Handles downloaded feed entries
	 *
	 * @param entries the downloaded entries
	 * @param handler the feed item handler
	 * @param cacheExpiryDate the date after which feed items expire and can be
	 * removed
	 * @param maxRunSeconds the maximum time application can run before being
	 * forcefully terminated
	 * @throws IOException if FeedItem constructor fails (e.g. unable to
	 * generate HTML based on the template)
	 * @throws TimeoutException if the task took too long to complete
	 */
	private void handleEntries(List<SyndEntry> entries, FeedItemHandler handler, Date cacheExpiryDate, int maxRunSeconds) throws IOException, TimeoutException {
		Set<FeedItem> newItems = new HashSet<>();
		for (SyndEntry entry : entries)
			newItems.add(new FeedItem(this, entry));

		Set<Key> deleteItems = new HashSet<>();
		final Set<Entity> saveItems = Collections.synchronizedSet(new HashSet<Entity>());

		//Find outdated items
		for (FeedItem oldItem : new HashSet<>(items))
			if (!newItems.contains(oldItem) && oldItem.getLastSeen() != null && oldItem.getLastSeen().before(cacheExpiryDate)) {
				//Item expired
				items.remove(oldItem);
				deleteItems.add(oldItem.getKey());
			} else if (newItems.contains(oldItem)) {
				for (FeedItem newItem : newItems)
					if (newItem.equals(oldItem))
						newItem.setState(oldItem.getState());//Transfer state to new item
				if (oldItem.getState() != FeedItem.State.SENT_PDF) {
					items.remove(oldItem);//Replace with new item to resend pdf
					deleteItems.add(oldItem.getKey());
				} else {
					oldItem.updateLastSeen();
					saveItems.add(oldItem.getEntity());
				}
			}

		// Delete items pending
		datastore.delete(deleteItems);
		deleteItems.clear();

		// Ignore already existing items
		newItems.removeAll(items);

		//Add new items
		items.addAll(newItems);
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), ThreadManager.currentRequestThreadFactory());
		for (FeedItem item : newItems) {
			executor.submit(new Runnable() {
				private FeedItemHandler handler;
				private Feed feed;
				private FeedItem item;

				public Runnable setParameters(FeedItemHandler handler, Feed feed, FeedItem item) {
					this.handler = handler;
					this.feed = feed;
					this.item = item;
					return this;
				}

				@Override
				public void run() {
					try {
						handler.handle(feed, item);
						saveItems.add(item.getEntity());
					} catch (RuntimeException ex) {
						log.error(MessageFormat.format(messages.getString("ERROR_HANDLING_FEED_ITEM"), new Object[]{item}), ex);
					}
				}
			}.setParameters(handler, this, item));
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(maxRunSeconds > 0 ? maxRunSeconds : Long.MAX_VALUE, TimeUnit.SECONDS)) {
				throw new TimeoutException(messages.getString("TIMED_OUT_WAITING_FOR_EXECUTOR"));
			}
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
		datastore.put(saveItems);
	}

	/**
	 * Downloads the feed and handles new or changed items
	 *
	 * @param handler the handler for feed items
	 * @param cacheExpiryDate the date after which feed items expire and can be
	 * removed
	 * @param maxRunSeconds the maximum time application can run before being
	 * forcefully terminated
	 */
	protected void update(FeedItemHandler handler, Date cacheExpiryDate, int maxRunSeconds) {
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.connect();
			SyndFeedInput feedInput = new SyndFeedInput();
			Charset charset = Charset.forName("utf-8"); //NOI18N
			if (connection.getContentEncoding() != null) {
				charset = Charset.forName(connection.getContentEncoding());
			} else if (connection.getContentType() != null) {
				String[] contentTypeParts = connection.getContentType().split(";"); //NOI18N
				for (String contentTypePart : contentTypeParts) {
					String[] contentTypePartComponents = contentTypePart.trim().split("=", 2); //NOI18N
					if (contentTypePartComponents.length == 2 && contentTypePartComponents[0].matches("charset")) //NOI18N
						charset = Charset.forName(contentTypePartComponents[1].trim());
				}
			}
			SyndFeed feed = feedInput.build(new InputStreamReader(connection.getInputStream(), charset));
			title = feed.getTitle();
			encoding = feed.getEncoding();
			handleEntries(feed.getEntries(), handler, cacheExpiryDate, maxRunSeconds);
		} catch (IOException | IllegalArgumentException | TimeoutException ex) {
			throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_UPDATE_FEED"), new Object[]{url}), ex);
		} catch (Exception ex) {
			throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_UPDATE_FEED"), new Object[]{url}), ex);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Feed
				&& ((Feed) obj).url.equals(url);
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 97 * hash + Objects.hashCode(this.url);
		return hash;
	}

	/*
	 * Getters
	 */
	/**
	 * Returns the feed title, as presented in the RSS download results
	 *
	 * @return the feed title, as presented in the RSS download results
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the feed items
	 *
	 * @return the feed items
	 */
	public Set<FeedItem> getItems() {
		return items;
	}

	/**
	 * Returns the feed title, as presented in the OPML file
	 *
	 * @return the feed title, as presented in the OPML file
	 */
	public String getUserTitle() {
		return StringUtils.join(userTitle, messages.getString("TITLE_SEPARATOR"));
	}

	/**
	 * Returns the feed URL
	 *
	 * @return the feed URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Returns the feed encoding
	 *
	 * @return the feed encoding
	 */
	public String getEncoding() {
		return (encoding != null && !encoding.isEmpty()) ? encoding : System.getProperty("file.encoding"); //NOI18N
	}
}
