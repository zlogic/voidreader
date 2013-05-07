/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.handler.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 *
 * @author Dmitry
 */
public class FileHandler extends AbstractPdfHandler implements ErrorHandler, FeedItemHandler {

	private static final int MAX_NAME = 50;
	private File feedItemsDir;

	public FileHandler(File feedItemsDir) {
		this.feedItemsDir = feedItemsDir;
	}

	@Override
	public void handle(Feed feed, Exception ex) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handle(Feed feed, FeedItem item) throws RuntimeException {
		String storageDirName = feed.getTitle().replaceAll("[/\n\r\t\0\f`?*\\<>|\":]", "!");
		//String storageDirName = feed.getTitle().replaceAll("[^a-zA-Z0-9 \\-_]", "!");
		if (storageDirName.length() >= MAX_NAME)
			storageDirName = storageDirName.substring(0, MAX_NAME);
		File tempDir = new File(feedItemsDir, storageDirName);
		if (!tempDir.exists())
			tempDir.mkdirs();
		try {
			File outputFile = File.createTempFile("html-", ".html", tempDir);
			try (PrintWriter writer = new PrintWriter(outputFile)) {
				writer.print(item.getItemHtml());
			}
			if (!item.isPdfSent()) {
				createPdf(tempDir, item);
				item.setPdfSent(true);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Cannot create temp file in " + tempDir.toString() + " for item " + item.getLink(), ex);
		}
	}

	private void createPdf(File parentDir, FeedItem item) throws Exception {
		File outputFilePdf = File.createTempFile("pdf-", ".pdf", parentDir);

		downloadRenderPdf(item.getLink()).createPDF(new FileOutputStream(outputFilePdf));
	}
}
