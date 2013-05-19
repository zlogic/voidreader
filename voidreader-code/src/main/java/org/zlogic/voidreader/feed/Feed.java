/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.feed;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import org.apache.commons.lang3.StringUtils;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.FetcherException;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 * A single RSS feed representation (with a cached list of feed items). Contains
 * annotations used by JAXB marshaling/unmarshaling and can be persisted in XML
 * form.
 *
 * Used to keep track of already downloaded/sent items.
 *
 * @author Dmitry Zolotukhin <zlogic@gmail.com>
 */
public class Feed {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * The logger
	 */
	private static final Logger log = Logger.getLogger(Feed.class.getName());
	/**
	 * The feed URL
	 */
	@XmlAttribute(name = "url")
	private String url;
	/**
	 * The feed items
	 */
	@XmlElement(name = "item")
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
	 * Empty constructor for JAXB
	 */
	private Feed() {
	}

	/**
	 * Creates the feed from data extracted form OPML
	 *
	 * @param url the feed URL
	 * @param userTitle the feed title as presented in the OPML
	 */
	protected Feed(String url, List<String> userTitle) {
		this.url = url;
		this.userTitle = new LinkedList<>(userTitle);
	}

	/**
	 * Constructs a feed based on an existing feed (e.g. loaded from OPML) and a
	 * list of feed items
	 *
	 * @param feed the existing feed
	 * @param items the feed items
	 */
	protected Feed(Feed feed, Set<FeedItem> items) {
		this(feed.url, feed.userTitle);
		this.items = items;
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
	private void handleEntries(List<Object> entries, FeedItemHandler handler, Date cacheExpiryDate, int maxRunSeconds) throws IOException, TimeoutException {
		if (items == null)
			items = new TreeSet<>();
		Set<FeedItem> newItems = new TreeSet<>();
		for (Object obj : entries)
			if (obj instanceof SyndEntry)
				newItems.add(new FeedItem(this, (SyndEntry) obj));

		//Find outdated items
		for (FeedItem oldItem : new TreeSet<>(items))
			if (!newItems.contains(oldItem) && oldItem.getLastSeen() != null && oldItem.getLastSeen().before(cacheExpiryDate)) {
				items.remove(oldItem);
			} else if (newItems.contains(oldItem)) {
				for (FeedItem newItem : newItems)
					if (newItem.equals(oldItem))
						newItem.setState(oldItem.getState());//Transfer state to new item
				if (oldItem.getState() != FeedItem.State.SENT_PDF)
					items.remove(oldItem);//Replace with new item to resend pdf
				else
					oldItem.updateLastSeen();
			}

		// Ignore already existing items
		newItems.removeAll(items);

		//Add new items
		items.addAll(newItems);
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//TODO: make this configurable
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
					} catch (RuntimeException ex) {
						log.log(Level.SEVERE, MessageFormat.format(messages.getString("ERROR_HANDLING_FEED_ITEM"), new Object[]{item}), ex);
						synchronized (items) {
							items.remove(item);
						}
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
	}

	/**
	 * Downloads the feed and handles new or changed items
	 *
	 * @param fetcher the ROME FeedFetcher
	 * @param handler the handler for feed items
	 * @param cacheExpiryDate the date after which feed items expire and can be
	 * removed
	 * @param maxRunSeconds the maximum time application can run before being
	 * forcefully terminated
	 * @throws RuntimeException if any error occur while handling this feed
	 */
	protected void update(FeedFetcher fetcher, FeedItemHandler handler, Date cacheExpiryDate, int maxRunSeconds) throws RuntimeException {
		try {
			SyndFeed feed = fetcher.retrieveFeed(new URL(url));
			title = feed.getTitle();
			encoding = feed.getEncoding();
			handleEntries(feed.getEntries(), handler, cacheExpiryDate, maxRunSeconds);
		} catch (FeedException | FetcherException | IOException | IllegalArgumentException | TimeoutException ex) {
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
