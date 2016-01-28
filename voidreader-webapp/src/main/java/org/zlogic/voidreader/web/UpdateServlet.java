package org.zlogic.voidreader.web;

import com.google.appengine.api.datastore.EntityNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.zlogic.voidreader.FeedDownloader;
import org.zlogic.voidreader.Settings;

/**
 * Servlet for updating the RSS feeds.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class UpdateServlet extends HttpServlet {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");

	/**
	 * Serves an HTTP GET request.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException if processing the response failed
	 * @throws IOException if unable to read the request or write the response
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if ("true".equalsIgnoreCase(request.getHeader("X-Appengine-Cron"))) { //NOI18N
			List<Settings> settingsList = Settings.loadAll();
			for (Settings settings : settingsList)
				new FeedDownloader().downloadFeeds(settings);
		} else {
			try {
				Settings settings = Settings.load(request.getUserPrincipal().getName());
				new FeedDownloader().downloadFeeds(settings);
			} catch (EntityNotFoundException ex) {
				throw new ServletException(messages.getString("YOU_NEED_TO_CONFIGURE_VOID_READER_FIRST"));
			}
		}
	}

}
