/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.web;

import com.google.appengine.api.datastore.EntityNotFoundException;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import org.zlogic.voidreader.Settings;

/**
 * Bean for getting the user settings for an HttpServletRequest.
 *
 * @author Dmitry Zolotukhin [zlogic@gmail.com]
 */
public class SettingsRequestBean {

	/**
	 * The associated Settings instance
	 */
	private Settings settings;

	/**
	 * Default constructor.
	 */
	public SettingsRequestBean() {

	}

	/**
	 * Sets the HttpServletRequest to obtain the user principal and associated
	 * Settings.
	 *
	 * @param request the request to use to obtain the Settings
	 */
	public void setRequest(HttpServletRequest request) {
		try {
			this.settings = Settings.load(request.getUserPrincipal().getName());
		} catch (EntityNotFoundException ex) {
			Properties properties = new Properties();
			properties.setProperty("email.to", request.getUserPrincipal().getName()); //NOI18N
			this.settings = new Settings(request.getUserPrincipal().getName(), properties);
		}
	}

	/**
	 * Returns the Settings for the request user principal.
	 *
	 * @return the Settings
	 */
	public Settings getSettings() {
		return settings;
	}
}
