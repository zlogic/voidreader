/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import java.io.File;
import java.util.ResourceBundle;

/**
 *
 * @author Dmitry
 */
public class Main {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");

	public static void main(String[] args) throws InterruptedException {
		if (args.length == 2) {
			FeedDownloader downloader = new FeedDownloader(new File(args[0]), new File(args[1]));
			downloader.downloadFeeds();
		}
	}
}
