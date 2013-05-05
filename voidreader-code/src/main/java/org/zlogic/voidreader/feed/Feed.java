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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
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
	@XmlAttribute(name = "url")
	private String url;
	@XmlElement(name = "item")
	private List<FeedItem> items;
	private String title;
	private String encoding;
	private List<String> userTitle;

	private Feed() {
	}

	protected Feed(String url, List<String> userTitle) {
		this.url = url;
		this.userTitle = new LinkedList<>(userTitle);
	}

	protected Feed(Feed feed, List<FeedItem> items) {
		this(feed.url, feed.userTitle);
		this.items = items;
	}

	private void handleEntries(List<Object> entries, FeedItemHandler handler) throws IOException {
		if (items == null)
			items = new LinkedList<>();
		List<FeedItem> newItems = new LinkedList<>();
		for (Object obj : entries)
			if (obj instanceof SyndEntry)
				newItems.add(new FeedItem(this, (SyndEntry) obj));

		//Find outdated items
		List<FeedItem> removeItems = new LinkedList<>();
		for (FeedItem oldItem : items)
			if (!newItems.contains(oldItem))
				removeItems.add(oldItem);

		// Ignore already existing items
		newItems.removeAll(items);

		//Discard outdated items
		items.removeAll(removeItems);
		//Add new items
		items.addAll(newItems);
		for (FeedItem item : newItems)
			handler.handle(this, item);
	}

	protected void update(FeedFetcher fetcher, FeedItemHandler handler) throws RuntimeException {
		try {
			SyndFeed feed = fetcher.retrieveFeed(new URL(url));
			title = feed.getTitle();
			encoding = feed.getEncoding();
			handleEntries(feed.getEntries(), handler);
		} catch (FeedException | FetcherException | IOException | IllegalArgumentException ex) {
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

	public List<FeedItem> getItems() {
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
