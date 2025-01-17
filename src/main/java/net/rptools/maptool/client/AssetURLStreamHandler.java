/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import net.rptools.lib.MD5Key;
import net.rptools.lib.image.ImageUtil;
import net.rptools.maptool.util.ImageManager;

/**
 * Support "asset://" in Swing components
 *
 * @author Azhrei
 */
public class AssetURLStreamHandler extends URLStreamHandler {

  @Override
  protected URLConnection openConnection(URL u) {
    return new AssetURLConnection(u);
  }

  private static class AssetURLConnection extends URLConnection {

    public AssetURLConnection(URL url) {
      super(url);
    }

    @Override
    public void connect() {
      // Nothing to do
    }

    @Override
    public InputStream getInputStream() throws IOException {
      String strAssetId = url.getHost();

      int scaleW = 0, scaleH = 0;
      // If assetId has "-", remove it and resize appropriately.
      int index = strAssetId.indexOf('-');
      if (index >= 0) {
        String szStr = strAssetId.substring(index + 1);
        strAssetId = strAssetId.substring(0, index);
        scaleW = scaleH = Integer.parseInt(szStr);
      }

      final MD5Key assetId = new MD5Key(strAssetId);
      String query = url.getQuery();
      Map<String, String> var = new HashMap<String, String>();

      while (query != null && query.length() > 1) {
        int delim = query.indexOf('=');
        if (delim > 0) {
          String name = query.substring(0, delim).trim();
          String value;
          int delim2 = query.indexOf('&', delim);
          if (delim2 < 0) {
            value = query.substring(delim + 1);
            query = null;
          } else {
            value = query.substring(delim + 1, delim2);
            query = query.substring(delim2 + 1);
          }
          var.put(name, value);
        } else break;
      }
      // Default value is 0: scale the dimension to preserve the aspect ratio
      // Use -1 to indicate that the original dimension from the image should be used
      scaleW = var.get("width") != null ? Integer.parseInt(var.get("width")) : scaleW;
      scaleH = var.get("height") != null ? Integer.parseInt(var.get("height")) : scaleH;

      BufferedImage img = ImageManager.getImageAndWait(assetId);

      if (scaleW > 0 || scaleH > 0) {
        switch (scaleW) {
          case -1:
            scaleW = img.getWidth();
            break;
          case 0:
            scaleW = img.getWidth() * scaleH / img.getHeight();
            break;
        }
        switch (scaleH) {
          case -1:
            scaleH = img.getHeight();
            break;
          case 0:
            scaleH = img.getHeight() * scaleW / img.getWidth();
            break;
        }
        BufferedImage bimg = new BufferedImage(scaleW, scaleH, BufferedImage.TRANSLUCENT);
        Graphics2D g = bimg.createGraphics();
        g.drawImage(img, 0, 0, scaleW, scaleH, null);
        g.dispose();
        img = bimg;
      }

      byte[] data = ImageUtil.imageToBytes(img, "png"); // assume png because translucent.

      return new ByteArrayInputStream(data);
    }
  }
}
