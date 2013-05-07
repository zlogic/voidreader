/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author Dmitry
 */
public class Feed {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	private static final Logger log = Logger.getLogger(Feed.class.getName());
	@XmlAttribute(name = "url")
	private String url;
	@XmlElement(name = "item")
	private Set<FeedItem> items;
	private String title;
	private String encoding;
	private List<String> userTitle;

	private Feed() {
	}

	protected Feed(String url, List<String> userTitle) {
		this.url = url;
		this.userTitle = new LinkedList<>(userTitle);
	}

	protected Feed(Feed feed, Set<FeedItem> items) {
		this(feed.url, feed.userTitle);
		this.items = items;
	}

	private void handleEntries(List<Object> entries, FeedItemHandler handler, Date cacheExpiryDate) throws IOException, TimeoutException {
		if (items == null)
			items = new TreeSet<>();
		Set<FeedItem> newItems = new TreeSet<>();
		for (Object obj : entries)
			if (obj instanceof SyndEntry)
				newItems.add(new FeedItem(this, (SyndEntry) obj));

		//Find outdated items
		for (FeedItem oldItem : new TreeSet<>(items))
			if (!newItems.contains(oldItem) && oldItem.getLastSeen().before(cacheExpiryDate)) {
				items.remove(oldItem);
			} else if (newItems.contains(oldItem)) {
				if (!oldItem.isPdfSent())
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
						log.log(Level.SEVERE, "Error handling feed item" + item, ex);
						synchronized (items) {
							items.remove(item);
						}
					}
				}
			}.setParameters(handler, this, item));
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
				//TODO: make timeout configurable
				throw new TimeoutException("Timed out waiting for executor");
			}
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected void update(FeedFetcher fetcher, FeedItemHandler handler, Date cacheExpiryDate) throws RuntimeException {
		try {
			SyndFeed feed = fetcher.retrieveFeed(new URL(url));
			title = feed.getTitle();
			encoding = feed.getEncoding();
			handleEntries(feed.getEntries(), handler, cacheExpiryDate);
		} catch (FeedException | FetcherException | IOException | IllegalArgumentException | TimeoutException ex) {
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
	public String getTitle() {
		return title;
	}

	public Set<FeedItem> getItems() {
		return items;
	}

	public String getUserTitle() {
		return StringUtils.join(userTitle, messages.getString("TITLE_SEPARATOR"));
	}

	public String getUrl() {
		return url;
	}

	public String getEncoding() {
		return (encoding != null && !encoding.isEmpty()) ? encoding : System.getProperty("file.encoding");
	}
}
