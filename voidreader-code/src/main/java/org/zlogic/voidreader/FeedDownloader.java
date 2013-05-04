/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.impl.HttpClientFeedFetcher;

/**
 *
 * @author Dmitry
 */
public class FeedDownloader {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	private Opml opml;
	private File opmlFile;
	private FeedFetcher feedFetcher = new HttpClientFeedFetcher();
	private List<Feed> feeds = new LinkedList<>();

	public FeedDownloader(File opmlFile) {
		this.opmlFile = opmlFile;
	}

	private List<Feed> loadFeeds(List outlines, List<String> parentTitles) {
		List<Feed> loadedFeeds = new LinkedList<>();
		if (parentTitles == null)
			parentTitles = new LinkedList<>();
		for (Object obj : outlines) {
			if (obj instanceof Outline) {
				Outline outline = (Outline) obj;
				List<String> currentParentTitles = new LinkedList<>(parentTitles);
				currentParentTitles.add(outline.getTitle());
				if ("rss".equals(outline.getType()) && outline.getXmlUrl() != null)
					loadedFeeds.add(new Feed(outline.getXmlUrl(), feedFetcher, currentParentTitles));
				loadedFeeds.addAll(loadFeeds(outline.getChildren(), currentParentTitles));
			}
		}
		return loadedFeeds;
	}

	private void loadOpml() throws FileNotFoundException, IOException, IllegalArgumentException, FeedException {
		if (opml == null) {
			opml = (Opml) new WireFeedInput().build(opmlFile);
			feeds = loadFeeds(opml.getOutlines(), null);
		}
	}

	public void downloadFeeds() {
		try {
			loadOpml();
		} catch (IOException | FeedException ex) {
			Logger.getLogger(FeedDownloader.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		for (Feed feed : feeds)
			try {
				feed.update();
			} catch (RuntimeException ex) {
				Logger.getLogger(FeedDownloader.class.getName()).log(Level.SEVERE, null, ex);
			}
	}
}
