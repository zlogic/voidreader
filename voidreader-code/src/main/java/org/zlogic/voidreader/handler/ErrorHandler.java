/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler;

import org.zlogic.voidreader.feed.Feed;

/**
 * Interface for classes which can handle an exception.
 *
 * @author Dmitry Zolotukhin <zlogic@gmail.com>
 */
public interface ErrorHandler {

	/**
	 * Handle an error or exception
	 *
	 * @param feed the feed which caused the error
	 * @param ex the exception to be handled
	 */
	public void handle(Feed feed, Exception ex);
}
