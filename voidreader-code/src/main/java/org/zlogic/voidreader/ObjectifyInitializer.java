/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader;

import com.googlecode.objectify.ObjectifyService;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;

/**
 * Class for initializing Objectify service.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class ObjectifyInitializer {

	static {
		ObjectifyService.register(Feed.class);
		ObjectifyService.register(FeedItem.class);
		ObjectifyService.register(Settings.class);
	}
}
