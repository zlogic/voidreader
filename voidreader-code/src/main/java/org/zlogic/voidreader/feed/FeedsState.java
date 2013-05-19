/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.feed;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.rometools.fetcher.FeedFetcher;
import org.zlogic.voidreader.Settings;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;
import org.zlogic.voidreader.handler.impl.EmailHandler;
import org.zlogic.voidreader.handler.impl.FileHandler;

/**
 * Toplevel class for handling all feeds: saving data to XML, restoring data
 * from XML, loading feeds list from OPML, downloading & handling new feed
 * items.
 *
 *
 * @author Dmitry Zolotukhin <zlogic@gmail.com>
 */
@XmlRootElement(name = "feeds")
public class FeedsState {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * The logger
	 */
	private static final Logger log = Logger.getLogger(FeedsState.class.getName());
	/**
	 * Feeds list
	 */
	@XmlElement(name = "feed")
	private List<Feed> feeds;
	/**
	 * File for storing feeds state
	 */
	private File persistenceFile;
	/**
	 * Error handler
	 */
	private ErrorHandler errorHandler;
	/**
	 * Feed item handler
	 */
	private FeedItemHandler feedItemHandler;
	/**
	 * The date after which feed items expire and can be removed
	 */
	private Date cacheExpiryDate;
	/**
	 * Maximum time application can run before being forcefully terminated
	 */
	private int maxRunSeconds;

	/**
	 * Constructor for FeedsState
	 *
	 * @param settings the application global settings
	 */
	public FeedsState(Settings settings) {
		this.persistenceFile = settings.getStorageFile();
		if (persistenceFile.exists())
			restoreDownloadedItems();
		switch (settings.getHandler()) {
			case SMTP:
			case IMAP:
				EmailHandler emailHandler = new EmailHandler(settings);
				errorHandler = emailHandler;
				feedItemHandler = emailHandler;
				break;
			case FILE:
				FileHandler fileHandler = new FileHandler(settings.getTempDir());
				errorHandler = fileHandler;
				feedItemHandler = fileHandler;
				break;
		}

		Calendar expiryDate = new GregorianCalendar();
		expiryDate.add(Calendar.DAY_OF_MONTH, -settings.getCacheExpireDays());
		cacheExpiryDate = expiryDate.getTime();
		this.maxRunSeconds = settings.getMaxRunSeconds();
	}

	/**
	 * Empty constructor for JAXB
	 */
	private FeedsState() {
	}

	/**
	 * Restores feed data from XML
	 *
	 * @throws RuntimeException if restoring failed
	 */
	private void restoreDownloadedItems() throws RuntimeException {
		Object obj = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(FeedsState.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			obj = jaxbUnmarshaller.unmarshal(persistenceFile);
		} catch (JAXBException ex) {
			throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_LOAD_SAVED_FEEDS_FROM_FILE"), new Object[]{persistenceFile.toString()}), ex);
		}
		if (obj instanceof FeedsState) {
			FeedsState savedFeeds = (FeedsState) obj;
			feeds = savedFeeds.feeds;
		}
	}

	/**
	 * Saves feed data to XML
	 *
	 * @throws RuntimeException if save failed
	 */
	protected void saveDownloadedItems() throws RuntimeException {
		synchronized (this) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(FeedsState.class);
				Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

				File tempFile = new File(persistenceFile.getParentFile(), persistenceFile.getName() + ".tmp"); //NOI18N
				jaxbMarshaller.marshal(this, tempFile);
				Files.move(Paths.get(tempFile.toURI()), Paths.get(persistenceFile.toURI()), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
			} catch (JAXBException | IOException ex) {
				throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_SAVE_FEEDS_TO_FILE"), new Object[]{persistenceFile.toString()}), ex);
			}
		}
	}

	/**
	 * Updates feed data by adding/removing items to match it with the OPML data
	 *
	 * @param opml the ROME OPML data
	 */
	public void updateOpml(Opml opml) {
		List<Feed> newFeeds = loadFeeds(opml.getOutlines(), null);
		if (feeds == null) {
			feeds = newFeeds;
		} else {
			//Remove items absent from OPML
			List<Feed> removeItems = new LinkedList<>();
			for (Feed feed : feeds)
				if (!newFeeds.contains(feed))
					removeItems.add(feed);
			feeds.removeAll(removeItems);
			//Update/add items from OPML
			for (Feed feed : newFeeds) {
				if (!feeds.contains(feed)) {
					feeds.add(feed);
				} else {
					Feed oldFeed = feeds.get(feeds.indexOf(feed));
					feeds.remove(oldFeed);
					feeds.add(new Feed(feed, oldFeed.getItems()));
				}
			}
		}
	}

	/**
	 * Recursively parses ROME OPML feed outlines
	 *
	 * @param outlines the feed outlines
	 * @param parentTitles the list of parent feed titles, or null if this is
	 * the top level
	 * @return the list of Feed instances
	 */
	private List<Feed> loadFeeds(List outlines, List<String> parentTitles) {
		List<Feed> loadedFeeds = new LinkedList<>();
		if (parentTitles == null)
			parentTitles = new LinkedList<>();
		for (Object obj : outlines) {
			if (obj instanceof Outline) {
				Outline outline = (Outline) obj;
				List<String> currentParentTitles = new LinkedList<>(parentTitles);
				currentParentTitles.add(outline.getTitle());
				if ("rss".equals(outline.getType()) && outline.getXmlUrl() != null) //NOI18N
					loadedFeeds.add(new Feed(outline.getXmlUrl(), currentParentTitles));
				loadedFeeds.addAll(loadFeeds(outline.getChildren(), currentParentTitles));
			}
		}
		return loadedFeeds;
	}

	/**
	 * Returns the list of feeds
	 *
	 * @return the list of feeds
	 */
	public List<Feed> getFeeds() {
		if (feeds == null)
			feeds = new LinkedList<>();
		return feeds;
	}

	/**
	 * Starts the shutdown watchdog to terminate the application if it's running
	 * too long
	 *
	 * @return the timer which can be cancelled if necessary
	 */
	private Timer scheduleShutdownTask() {
		if (maxRunSeconds <= 0)
			return null;
		Timer terminateTimer = new Timer("TerminateTimer", true); //NOI18N
		TimerTask terminateTimerTask = new TimerTask() {
			@Override
			public void run() {
				log.severe(messages.getString("APPLICATION_RAN_OUT_OF_TIME_FORCING_SHUTDOWN"));
				saveDownloadedItems();
				System.exit(-1);
			}
		};
		Calendar terminateDate = new GregorianCalendar();
		terminateDate.add(Calendar.SECOND, (int) maxRunSeconds);

		terminateTimer.schedule(terminateTimerTask, terminateDate.getTime());
		return terminateTimer;
	}

	/**
	 * Downloads the latest feed data and handles new and updated items
	 *
	 * @param feedFetcher the ROME FeedFetcher instance
	 * @throws RuntimeException if a major error occurred while processing the
	 * feeds
	 * @throws TimeoutException if the task took too long to complete
	 */
	public void update(FeedFetcher feedFetcher) throws RuntimeException, TimeoutException {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);//TODO: make this configurable
		Timer terminateTimer = scheduleShutdownTask();
		for (Feed feed : feeds) {
			executor.submit(new Runnable() {
				private FeedFetcher feedFetcher;
				private Feed feed;

				public Runnable setParameters(Feed feed, FeedFetcher feedFetcher) {
					this.feed = feed;
					this.feedFetcher = feedFetcher;
					return this;
				}

				@Override
				public void run() {
					try {
						feed.update(feedFetcher, feedItemHandler, cacheExpiryDate, maxRunSeconds);
					} catch (RuntimeException ex) {
						log.log(Level.SEVERE, null, ex);//TODO: use an error handler
					}
				}
			}.setParameters(feed, feedFetcher));
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(maxRunSeconds > 0 ? maxRunSeconds : Long.MAX_VALUE, TimeUnit.SECONDS)) {
				throw new TimeoutException(messages.getString("TIMED_OUT_WAITING_FOR_EXECUTOR"));
			}
		} catch (InterruptedException ex) {
			saveDownloadedItems();
			throw new RuntimeException(ex);
		}
		if (terminateTimer != null)
			terminateTimer.cancel();
		saveDownloadedItems();
	}
}
