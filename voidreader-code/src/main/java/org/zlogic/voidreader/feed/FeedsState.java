/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.feed;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
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
 *
 * @author Dmitry
 */
@XmlRootElement(name = "feeds")
public class FeedsState {

	@XmlElement(name = "feed")
	private List<Feed> feeds;
	private static final Logger log = Logger.getLogger(FeedsState.class.getName());
	private File persistenceFile;
	private ErrorHandler errorHandler;
	private FeedItemHandler feedItemHandler;

	public FeedsState(Settings settings) {
		this.persistenceFile = settings.getStorageFile();
		if (persistenceFile.exists())
			restoreDownloadedItems(persistenceFile);
		switch (settings.getHandler()) {
			case SMTP:
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
	}

	private FeedsState() {
	}

	private void restoreDownloadedItems(File persistenceFile) throws RuntimeException {
		Object obj = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(FeedsState.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			obj = jaxbUnmarshaller.unmarshal(persistenceFile);
		} catch (JAXBException ex) {
			throw new RuntimeException("Cannot load saved feeds from file " + persistenceFile.toString(), ex);
		}
		if (obj instanceof FeedsState) {
			FeedsState savedFeeds = (FeedsState) obj;
			feeds = savedFeeds.feeds;
		}
	}

	protected void saveDownloadedItems() throws PropertyException, JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(FeedsState.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMarshaller.marshal(this, persistenceFile);
	}

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

	private List<Feed> loadFeeds(List outlines, List<String> parentTitles) {
		List<Feed> loadedFeeds = new LinkedList<>();
		if (parentTitles == null)
			parentTitles = new LinkedList<>();
		for (Object obj : outlines) {
			if (obj instanceof Outline) {
				Outline outline = (Outline) obj;
				List<String> currentParentTitles = new LinkedList<>(parentTitles);
				currentParentTitles.add(outline.getTitle());
				if ("rss".equals(outline.getType()) && outline.getXmlUrl() != null)
					loadedFeeds.add(new Feed(outline.getXmlUrl(), currentParentTitles));
				loadedFeeds.addAll(loadFeeds(outline.getChildren(), currentParentTitles));
			}
		}
		return loadedFeeds;
	}

	public List<Feed> getFeeds() {
		if (feeds == null)
			feeds = new LinkedList<>();
		return feeds;
	}

	public void update(FeedFetcher feedFetcher) throws RuntimeException, TimeoutException {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//TODO: make this configurable
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
						feed.update(feedFetcher, feedItemHandler);
					} catch (RuntimeException ex) {
						log.log(Level.SEVERE, null, ex);//TODO: use an error handler
					}
				}
			}.setParameters(feed, feedFetcher));
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
				//TODO: make this configurable
				throw new TimeoutException("Timed out waiting for executor");
			}
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
		try {
			saveDownloadedItems();
		} catch (JAXBException ex) {
			throw new RuntimeException("Error while saving feed persistence", ex);
		}
	}
}
