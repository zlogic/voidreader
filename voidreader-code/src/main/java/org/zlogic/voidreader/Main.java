/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dmitry
 */
public class Main {

	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	private static final Logger log = Logger.getLogger(FeedDownloader.class.getName());

	/**
	 * Performs initialization of application's dependencies
	 */
	private void initApplication() {
		//Configure logging to load config from classpath
		String loggingFile = System.getProperty("java.util.logging.config.file"); //NOI18N
		if (loggingFile == null || loggingFile.isEmpty()) {
			try {
				java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("logging.properties"); //NOI18N
				if (url != null)
					java.util.logging.LogManager.getLogManager().readConfiguration(url.openStream());
			} catch (IOException | SecurityException e) {
				log.log(Level.SEVERE, messages.getString("ERROR_WHEN_LOADING_LOGGING_CONFIGURATION"), e);
				System.err.println(messages.getString("ERROR_WHEN_LOADING_LOGGING_CONFIGURATION"));
			}
		}
	}

	public static void main(String[] args) {
		new Main().initApplication();
		/*
		 * Use http://commons.apache.org/proper/commons-cli/usage.html ?
		 */
		try {
			if (args.length == 1) {
				FeedDownloader downloader = new FeedDownloader(new Properties(new File(args[0])));
				downloader.downloadFeeds();
			}
		} catch (Throwable thr) {
			log.log(Level.SEVERE, "Caught exception", thr);
		}
	}
}
