/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import java.io.File;

/**
 *
 * @author Dmitry
 */
public class Main {

	public static void main(String[] args) {
		if (args.length == 1)
			new FeedDownloader(new File(args[0])).downloadFeeds();
	}
}
