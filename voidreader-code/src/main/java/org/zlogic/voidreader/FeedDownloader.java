/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic42@outlook.com>
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.impl.HttpClientFeedFetcher;
import org.zlogic.voidreader.feed.FeedsState;

/**
 * Class used to launch the feed download process
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic42@outlook.com">zlogic42@outlook.com</a>
 */
public class FeedDownloader {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * OPML file
	 */
	private File opmlFile;
	/**
	 * ROME feed fetcher
	 */
	private FeedFetcher feedFetcher = new HttpClientFeedFetcher();
	/**
	 * FeedsState instance
	 */
	private FeedsState feedData;

	/**
	 * Constructor for FeedDownloader
	 *
	 * @param settings the Settings class instance
	 */
	public FeedDownloader(Settings settings) {
		feedData = new FeedsState(settings);
		this.opmlFile = settings.getOpmlFile();
	}

	/**
	 * Downloads feeds, handles new items, saves the feed state.
	 *
	 * @throws RuntimeException when a critical exception occurs
	 */
	public void downloadFeeds() throws RuntimeException {
		try {
			feedData.updateOpml((Opml) new WireFeedInput().build(opmlFile));
			feedData.update(feedFetcher);
		} catch (IOException | IllegalArgumentException | FeedException | TimeoutException ex) {
			throw new RuntimeException(messages.getString("ERROR_WHILE_DOWNLOADING_FEEDS"), ex);
		}
	}
}
