/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.impl.HttpClientFeedFetcher;
import org.zlogic.voidreader.feed.FeedsState;

/**
 *
 * @author Dmitry
 */
public class FeedDownloader {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	private File opmlFile;
	private FeedFetcher feedFetcher = new HttpClientFeedFetcher();
	private FeedsState feedData;

	public FeedDownloader(File opmlFile, File stateFile) {
		feedData = new FeedsState(stateFile);//TODO: make this configurable
		this.opmlFile = opmlFile;
	}

	public void downloadFeeds() {
		try {
			feedData.updateOpml((Opml) new WireFeedInput().build(opmlFile));
		} catch (IOException | FeedException | RuntimeException ex) {
			Logger.getLogger(FeedDownloader.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		feedData.update(feedFetcher);
	}
}
