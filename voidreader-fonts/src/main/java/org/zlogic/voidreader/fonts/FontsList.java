/*
 * Void Reader project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.voidreader.fonts;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.IOUtils;

/**
 * Helper class to retrieve the list of fonts included in this package
 *
 * @author Dmitry Zolotukhin <a href="mailto:zlogic42@outlook.com">zlogic42@outlook.com</a>
 */
public class FontsList {

	/**
	 * Returns a list of URLs with fonts contained in this package
	 *
	 * @return the list of URLs with fonts contained in this package
	 * @throws IOException if creating the font list failed
	 */
	public URL[] getFontUrls() throws IOException {
		String[] fonts = IOUtils.toString(FontsList.class.getResourceAsStream("fonts.txt")).split("[\r\n]+"); //NOI18N
		URL[] fontsUrls = new URL[fonts.length];
		for (int i = 0; i < fonts.length; i++)
			fontsUrls[i] = FontsList.class.getResource(fonts[i]);
		return fontsUrls;
	}
}
