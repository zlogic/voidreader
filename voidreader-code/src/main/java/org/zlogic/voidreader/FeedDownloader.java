/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.FetcherException;
import org.rometools.fetcher.impl.HttpClientFeedFetcher;

/**
 *
 * @author Dmitry
 */
public class FeedDownloader {

	private Opml opml;
	private File opmlFile;
	private FeedFetcher feedFetcher = new HttpClientFeedFetcher();

	public FeedDownloader(File opmlFile) {
		this.opmlFile = opmlFile;
	}

	protected void handleEntry(SyndFeed feed, SyndEntry entry) {
		String feedTitle = feed.getTitle();
		for (Object obj : entry.getContents())
			if (obj instanceof SyndContent) {
				SyndContent content = (SyndContent) obj;
				String value = content.getValue();
			}
	}

	protected void downloadFeed(String url) throws RuntimeException {
		SyndFeed feed = null;
		try {
			feed = feedFetcher.retrieveFeed(new URL(url));
		} catch (FeedException | FetcherException | IOException | RuntimeException ex) {
			throw new RuntimeException("Cannot download feed " + url, ex);
		}
		for (Object obj : feed.getEntries())
			if (obj instanceof SyndEntry)
				handleEntry(feed, (SyndEntry) obj);
	}

	private void downloadFeeds(List outlines) {
		for (Object obj : outlines) {
			if (obj instanceof Outline) {
				Outline outline = (Outline) obj;
				String title = outline.getTitle();
				if ("rss".equals(outline.getType()) && outline.getXmlUrl() != null)
					try {
						downloadFeed(outline.getXmlUrl());
					} catch (Exception ex) {
						Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
					}
				downloadFeeds(outline.getChildren());
			}
		}
	}

	private void loadOpml() throws FileNotFoundException, IOException, IllegalArgumentException, FeedException {
		if (opml == null)
			opml = (Opml) new WireFeedInput().build(opmlFile);
	}

	public void downloadFeeds() {
		try {
			loadOpml();
		} catch (IOException | FeedException ex) {
			Logger.getLogger(FeedDownloader.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		downloadFeeds(opml.getOutlines());
	}
}
