/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.FetcherException;

/**
 *
 * @author Dmitry
 */
public class Feed {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	private String title;
	private List<FeedItem> items;
	private String encoding;
	private List<String> userTitle;
	private String url;
	private FeedFetcher feedFetcher;

	protected Feed(String url, FeedFetcher feedFetcher, List<String> userTitle) {
		this.url = url;
		this.feedFetcher = feedFetcher;
		this.userTitle = new LinkedList<>(userTitle);
	}

	private void handleEntries(List<Object> entries) throws IOException {
		if (items == null)
			items = new LinkedList<>();
		List<FeedItem> newItems = new LinkedList<>();
		for (Object obj : entries)
			if (obj instanceof SyndEntry)
				newItems.add(new FeedItem(this, (SyndEntry) obj));

		// Discard already existing items
		newItems.removeAll(items);

		//Find outdated items
		List<FeedItem> removeItems = new LinkedList<>();
		for (FeedItem oldItem : items)
			if (!newItems.contains(oldItem))
				removeItems.add(oldItem);
		{
			//Discard outdated items
			items.removeAll(removeItems);
			//Add new items
			items.addAll(newItems);
			for (FeedItem item : newItems)
				handleNewEntry(item);
		}
	}

	private void handleNewEntry(FeedItem item) {
		File tempDir = new File("temp");
		for (String dir : userTitle)
			tempDir = new File(tempDir, dir.replaceAll("[/\n\r\t\0\f`?*\\<>|\":]", "$"));
		tempDir.mkdirs();
		try {
			File outputFile = File.createTempFile("html-", ".html", tempDir);
			try (PrintWriter writer = new PrintWriter(outputFile)) {
				writer.print(item.getItemHtml());
			}
		} catch (IOException ex) {
			Logger.getLogger(Feed.class.getName()).log(Level.SEVERE, "Cannot create temp file in " + tempDir.toString(), ex);
		}
	}

	public void update() throws RuntimeException {
		try {
			SyndFeed feed = feedFetcher.retrieveFeed(new URL(url));
			title = feed.getTitle();
			encoding = feed.getEncoding();
			handleEntries(feed.getEntries());
		} catch (FeedException | FetcherException | IOException ex) {
			throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_UPDATE_FEED"), new Object[]{url, ex}), ex);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Feed
				&& ((Feed) obj).url.equals(url);
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
