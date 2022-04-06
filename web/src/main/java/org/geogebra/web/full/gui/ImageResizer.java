package org.geogebra.web.full.gui;

import java.util.function.Consumer;

import org.geogebra.web.html5.gui.util.Dom;

import elemental2.dom.CanvasRenderingContext2D;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.HTMLImageElement;
import jsinterop.base.Js;

/**
 * Utility class for resizing Images
 *
 */
public class ImageResizer {

	/**
	 * Resizes an Image not keeping the aspect ratio
	 * 
	 * @param imgDataURL
	 *            the data URL of the image
	 * @param width
	 *            width of the resized image
	 * @param height
	 *            height of the resized image
	 * @param callback accepts the data URL of the resized image or the original data URL in
	 *         case of no resize happened
	 */
	public static void resizeImage(String imgDataURL, int width, int height,
			Consumer<String> callback) {
		HTMLImageElement image = Dom.createImage();
		image.addEventListener("load", event -> {
			int sWidth = image.width;
			int sHeight = image.height;

			if (!(sWidth == width && sHeight == height)) {
				HTMLCanvasElement canvasTmp =
						(HTMLCanvasElement) DomGlobal.document.createElement("canvas");
				CanvasRenderingContext2D context = Js.uncheckedCast(
						canvasTmp.getContext("2d"));
				canvasTmp.width = width;
				canvasTmp.height = height;

				context.drawImage(image, 0, 0, sWidth, sHeight, 0, 0, width, height);
				callback.accept(canvasTmp.toDataURL());
			} else {
				callback.accept(imgDataURL);
			}
		});
		image.src = imgDataURL;
	}

}
