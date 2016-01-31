/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader;

import com.itextpdf.text.DocumentException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import org.junit.Ignore;
import org.junit.Test;
import org.zlogic.voidreader.handler.impl.AbstractPdfHandler;
import org.zlogic.voidreader.handler.impl.HTMLDownloaderCleaner;

/**
 * Test downloading of linked pages.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
@Ignore
public class TestDownloading {

	/**
	 * Downloads a page in HTML and PDF formats for review by user. Shoudln't
	 * crash or fail.
	 *
	 * @throws IOException on failure
	 * @throws DocumentException on failure
	 */
	@Test
	public void testDownload() throws IOException, DocumentException {
		new HTMLDownloaderCleaner() {

			public void run() throws IOException, DocumentException {
				new PrintWriter("target/test.html", "utf-8").write(downloadCleanHTML(new URL("https://linux.org.ru")));
			}
		}.run();
		new AbstractPdfHandler() {
			public void run() throws IOException, DocumentException {
				new FileOutputStream("target/test.pdf").write(downloadRenderPdf(new URL("https://linux.org.ru")));
			}
		}.run();
	}
}
