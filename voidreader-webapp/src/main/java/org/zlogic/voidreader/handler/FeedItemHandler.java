/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler;

import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;

/**
 * Interface for classes which can handle a new or changed feed item.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public interface FeedItemHandler {

	/**
	 * Handle a new or updated feed item
	 *
	 * @param feed the source feed
	 * @param item the new or updated feed item
	 */
	public void handle(Feed feed, FeedItem item);
}
