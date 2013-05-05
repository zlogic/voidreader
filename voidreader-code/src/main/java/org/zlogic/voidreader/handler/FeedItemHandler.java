/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.handler;

import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;

/**
 *
 * @author Dmitry
 */
public interface FeedItemHandler {

	public void handle(Feed feed, FeedItem item) throws RuntimeException;
}
