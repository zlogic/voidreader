/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.zlogic.voidreader.fonts;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Dmitry
 */
public class FontsList {

	public URL[] getFontUrls() throws IOException {
		String[] fonts = IOUtils.toString(FontsList.class.getResourceAsStream("fonts.txt")).split("[\r\n]+");
		URL[] fontsUrls = new URL[fonts.length];
		for (int i = 0; i < fonts.length; i++)
			fontsUrls[i] = FontsList.class.getResource(fonts[i]);
		return fontsUrls;
	}
}
