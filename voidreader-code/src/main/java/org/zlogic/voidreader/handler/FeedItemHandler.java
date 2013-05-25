/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic42@outlook.com>
 */
package org.zlogic.voidreader.handler;

import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;

/**
 * Interface for classes which can handle a new or changed feed item.
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic42@outlook.com">zlogic42@outlook.com</a>
 */
public interface FeedItemHandler {

	/**
	 * Handle a new or updated feed item
	 *
	 * @param feed the source feed
	 * @param item the new or updated feed item
	 * @throws RuntimeException if an error occurred while processing the feed
	 * item
	 */
	public void handle(Feed feed, FeedItem item) throws RuntimeException;
}
