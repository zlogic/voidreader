/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.handler.file;

import com.itextpdf.text.DocumentException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.zlogic.voidreader.feed.Feed;
import org.zlogic.voidreader.feed.FeedItem;
import org.zlogic.voidreader.handler.ErrorHandler;
import org.zlogic.voidreader.handler.FeedItemHandler;

/**
 *
 * @author Dmitry
 */
public class FileHandler implements ErrorHandler, FeedItemHandler {

	private static final int MAX_NAME = 100;
	private File feedItemsDir;

	public FileHandler(File feedItemsDir) {
		this.feedItemsDir = feedItemsDir;
	}

	@Override
	public void handle(Feed feed, Exception ex) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handle(Feed feed, FeedItem item) {
		String storageDirName = feed.getTitle().replaceAll("[/\n\r\t\0\f`?*\\<>|\":]", "$");
		if (storageDirName.length() >= MAX_NAME)
			storageDirName = storageDirName.substring(0, MAX_NAME);
		File tempDir = new File(feedItemsDir, storageDirName);
		tempDir.mkdirs();
		try {
			File outputFile = File.createTempFile("html-", ".html", tempDir);
			try (PrintWriter writer = new PrintWriter(outputFile)) {
				writer.print(item.getItemHtml());
			}
			createPdf(tempDir, item);
		} catch (IOException ex) {
			Logger.getLogger(Feed.class.getName()).log(Level.SEVERE, "Cannot create temp file in " + tempDir.toString(), ex);
		} catch (Exception ex) {
			Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void createPdf(File parentDir, FeedItem item) throws IOException, DocumentException {
		/*
		 * See
		 * http://stackoverflow.com/questions/10493837/how-to-export-html-page-to-pdf-format
		 * http://stackoverflow.com/questions/235851/using-itext-to-convert-html-to-pdf
		 */
		File outputFilePdf = File.createTempFile("pdf-", ".pdf", parentDir);
		Document doc;
		HttpClient httpClient = new HttpClient();
		HttpMethod getMethod = new GetMethod(item.getLink());
		try {
			httpClient.executeMethod(getMethod);
			Tidy tidy = new Tidy();
			tidy.setXmlOut(true);
			tidy.setQuiet(true);
			tidy.setDropProprietaryAttributes(true);
			tidy.setShowErrors(0);
			tidy.setShowWarnings(false);
			doc = tidy.parseDOM(getMethod.getResponseBodyAsStream(), null);
		} finally {
			getMethod.releaseConnection();
		}

		//Convert to PDF
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocument(doc, item.getLink());
		renderer.layout();
		renderer.createPDF(new FileOutputStream(outputFilePdf));
	}
}
