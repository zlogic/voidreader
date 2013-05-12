/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.handler.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 * FeedItemHandler and ErrorHandler implementation which saves items in a
 * directory.
 *
 * WARNING: this is for testing purposes only. This class may fail with creating
 * files if it encounters unsupported characters in directory names; item names
 * are cryptic; previously downloaded items are automatically deleted.
 *
 * @author Dmitry Zolotukhin <zlogic@gmail.com>
 */
public class FileHandler extends AbstractPdfHandler implements ErrorHandler, FeedItemHandler {

	/**
	 * The logger
	 */
	private static final Logger log = Logger.getLogger(FileHandler.class.getName());
	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * Maximum length for filenames
	 */
	private static final int MAX_NAME = 50;
	/**
	 * Path where downloaded items will be saved
	 */
	private File feedItemsDir;

	/**
	 * Constructor for FileHandler.
	 *
	 * WARNING: Deletes all previously downloaded items!
	 *
	 * @param feedItemsDir the directory where to download feed items and save
	 * PDFs
	 */
	public FileHandler(File feedItemsDir) {
		this.feedItemsDir = feedItemsDir;
		try {
			deleteTree(Paths.get(feedItemsDir.toURI()));
		} catch (IOException ex) {
			log.log(Level.FINEST, null, ex);
		}
		/*
		 Runnable deleteFile = new Runnable() {
		 @Override
		 public void run() {
		 try {
		 deleteTree(Paths.get(tempDir.toURI()));
		 } catch (IOException ex) {
		 log.log(Level.SEVERE, null, ex);
		 }
		 }
		 };
		 Runtime.getRuntime().addShutdownHook(new Thread(deleteFile));
		 */
	}

	/**
	 * Recursively deletes a path
	 *
	 * @param parent the parent path
	 * @throws IOException if deletion fails
	 */
	private void deleteTree(Path parent) throws IOException {
		Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else
					throw exc;
			}
		});
	}

	@Override
	public void handle(Feed feed, Exception ex) {
		throw new UnsupportedOperationException(messages.getString("NOT_SUPPORTED_YET")); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handle(Feed feed, FeedItem item) throws RuntimeException {
		String storageDirName = feed.getTitle().replaceAll("[/\n\r\t\0\f`?*\\<>|\":]", "!"); //NOI18N
		//String storageDirName = feed.getTitle().replaceAll("[^a-zA-Z0-9 \\-_]", "!");
		if (storageDirName.length() >= MAX_NAME)
			storageDirName = storageDirName.substring(0, MAX_NAME);
		File tempDir = new File(feedItemsDir, storageDirName);
		if (!tempDir.exists())
			tempDir.mkdirs();
		try {
			File outputFile = File.createTempFile("html-", ".html", tempDir); //NOI18N
			try (PrintWriter writer = new PrintWriter(outputFile)) {
				writer.print(item.getItemHtml());
			}
			if (item.getState() != FeedItem.State.SENT_PDF) {
				createPdf(tempDir, item);
				item.setState(FeedItem.State.SENT_PDF);
			}
			if (item.getState() != FeedItem.State.SENT_PDF)
				item.setState(FeedItem.State.SENT_ENTRY);
		} catch (Exception ex) {
			throw new RuntimeException(MessageFormat.format(messages.getString("CANNOT_CREATE_TEMP_FILE_FOR_ITEM"), new Object[]{tempDir.toString(), item.getLink()}), ex);
		}
	}

	/**
	 * Downloads and saves a feed item's contents as a PDF file
	 *
	 * @param parentDir the directory where to create the PDF file
	 * @param item the feed item to save
	 * @throws Exception if PDF download or rendering failed
	 */
	private void createPdf(File parentDir, FeedItem item) throws Exception {
		File outputFilePdf = File.createTempFile("pdf-", ".pdf", parentDir); //NOI18N

		downloadRenderPdf(item.getLink()).createPDF(new FileOutputStream(outputFilePdf));
	}
}
