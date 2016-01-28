/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader;

import com.rometools.opml.feed.opml.Opml;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import org.zlogic.voidreader.feed.FeedsState;

/**
 * Class used to launch the feed download process
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class FeedDownloader {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");

	/**
	 * Downloads feeds, handles new items, saves the feed state.
	 *
	 * @param settings the user settings
	 */
	public void downloadFeeds(Settings settings) {
		try (Reader sourceReader = new StringReader(settings.getOpml())) {
			FeedsState feedData = new FeedsState(settings);
			feedData.updateOpml((Opml) new WireFeedInput().build(sourceReader));
			feedData.update();
		} catch (IOException | IllegalArgumentException | FeedException | TimeoutException ex) {
			throw new RuntimeException(messages.getString("ERROR_WHILE_DOWNLOADING_FEEDS"), ex);
		}
	}
}
