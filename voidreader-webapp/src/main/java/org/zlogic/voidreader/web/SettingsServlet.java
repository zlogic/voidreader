package org.zlogic.voidreader.web;

import com.google.appengine.api.datastore.EntityNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
		try {
			Settings settings = Settings.load(request.getUserPrincipal().getName());
			try (PrintWriter pw = new PrintWriter(response.getOutputStream())) {
				pw.print(settings.toString());
			}
		} catch (EntityNotFoundException ex) {
			throw new ServletException(messages.getString("YOU_NEED_TO_CONFIGURE_VOID_READER_FIRST"));
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
		for (Object key : request.getParameterMap().keySet())
			properties.put(key, request.getParameter(key.toString()));
		Settings settings = new Settings(request.getUserPrincipal().getName(), properties);
		settings.save();
		try (PrintWriter pw = new PrintWriter(response.getOutputStream())) {
			pw.print(settings.toString());
		}
	}

}
