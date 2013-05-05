/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.impl.HttpClientFeedFetcher;
import org.zlogic.voidreader.feed.FeedsState;

/**
 *
 * @author Dmitry
 */
public class FeedDownloader {

	private File opmlFile;
	private FeedFetcher feedFetcher = new HttpClientFeedFetcher();
	private FeedsState feedData;

	public FeedDownloader(Properties properties) {
		feedData = new FeedsState(properties.getStorageFile(), properties.getTempDir());
		this.opmlFile = properties.getOpmlFile();
	}

	public void downloadFeeds() throws FileNotFoundException, IOException, IllegalArgumentException, FeedException {
		feedData.updateOpml((Opml) new WireFeedInput().build(opmlFile));
		feedData.update(feedFetcher);
	}
}
