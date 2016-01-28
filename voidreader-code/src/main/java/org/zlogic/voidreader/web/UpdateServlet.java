package org.zlogic.voidreader.web;

import com.googlecode.objectify.Key;
import static com.googlecode.objectify.ObjectifyService.ofy;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.zlogic.voidreader.FeedDownloader;
import org.zlogic.voidreader.ObjectifyInitializer;
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
	 * Enforces ObjectifyInitializer to run
	 */
	private final ObjectifyInitializer oi = new ObjectifyInitializer();

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
			List<Settings> settingsList = ofy().load().type(Settings.class).list();
			for (Settings settings : settingsList)
				new FeedDownloader().downloadFeeds(settings);
		} else {
			Settings settings = ofy().load().now(Key.create(Settings.class, request.getUserPrincipal().getName()));
			if (settings == null)
				throw new ServletException(messages.getString("YOU_NEED_TO_CONFIGURE_VOID_READER_FIRST"));
			new FeedDownloader().downloadFeeds(settings);
		}
	}

}
