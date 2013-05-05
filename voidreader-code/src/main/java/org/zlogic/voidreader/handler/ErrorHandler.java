/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.handler;

import org.zlogic.voidreader.feed.Feed;

/**
 *
 * @author Dmitry
 */
public interface ErrorHandler {

	public void handle(Feed feed, Exception ex);
}
