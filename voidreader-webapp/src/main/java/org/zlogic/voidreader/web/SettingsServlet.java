package org.zlogic.voidreader.web;

import com.googlecode.objectify.Key;
import static com.googlecode.objectify.ObjectifyService.ofy;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.zlogic.voidreader.ObjectifyInitializer;
import org.zlogic.voidreader.Settings;

/**
 * Servlet for updating the RSS feeds.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class SettingsServlet extends HttpServlet {

	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/voidreader/messages");
	/**
	 * Enforces ObjectifyInitializer to run
	 */
	private final ObjectifyInitializer oi = new ObjectifyInitializer();

	/**
	 * Serves an HTTP POST request.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException if processing the response failed
	 * @throws IOException if unable to read the request or write the response
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("utf-8"); //NOI18N
		response.setContentType("text/plain; charset=UTF-8"); //NOI18N
		request.setCharacterEncoding("utf-8"); //NOI18N
		Settings settings = ofy().load().now(Key.create(Settings.class, request.getUserPrincipal().getName()));
		if (settings == null)
			throw new ServletException(messages.getString("YOU_NEED_TO_CONFIGURE_VOID_READER_FIRST"));
		try (PrintWriter pw = new PrintWriter(response.getOutputStream())) {
			pw.print(settings.toString());
		}
	}

	/**
	 * Serves an HTTP POST request.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException if processing the response failed
	 * @throws IOException if unable to read the request or write the response
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("utf-8"); //NOI18N
		response.setContentType("text/plain; charset=UTF-8"); //NOI18N
		request.setCharacterEncoding("utf-8"); //NOI18N
		Properties properties = new Properties();
		properties.putAll(request.getParameterMap());
		Settings settings = new Settings(request.getUserPrincipal().getName(), properties);
		ofy().save().entity(settings).now();
		try (PrintWriter pw = new PrintWriter(response.getOutputStream())) {
			pw.print(settings.toString());
		}
	}

}
