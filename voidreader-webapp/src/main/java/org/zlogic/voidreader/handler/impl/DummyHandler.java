/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler.impl;

import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 * FeedItemHandler and ErrorHandler implementation which marks an item as
 * "handled".
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class DummyHandler implements FeedItemHandler, ErrorHandler {

	/**
	 * The logger
	 */
	private static final Logger log = LoggerFactory.getLogger(EmailHandler.class);
	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");

	@Override
	public void handle(Feed feed, FeedItem item) {
		item.setState(FeedItem.State.SENT_PDF);
		log.info(messages.getString("MARKED_ITEM_STATE_FROM_FEED_AS"), item.getTitle(), item.getLink(), feed.getTitle(), feed.getUrl(), item.getState());
	}

	@Override
	public void handle(Feed feed, Exception ex) {
		throw new UnsupportedOperationException(messages.getString("NOT_SUPPORTED_YET")); //To change body of generated methods, choose Tools | Templates.
	}

}
