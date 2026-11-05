package util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PcxLoader {
	private static final int HEADER_SIZE = 128;
	private static final int PALETTE_SIZE = 768;

	public static BufferedImage load(String path) throws IOException {
		byte[] data = Files.readAllBytes(Paths.get(path));

		int bitsPerPixel = data[3] & 0xFF;
		int planes = data[65] & 0xFF;
		if ((data[0] & 0xFF) != 10 || bitsPerPixel != 8 || planes != 1) {
			throw new IOException("Formato PCX nao suportado: " + path);
		}

		int width = readShort(data, 8) - readShort(data, 4) + 1;
		int height = readShort(data, 10) - readShort(data, 6) + 1;
		int bytesPerLine = readShort(data, 66);
		int paletteOffset = data.length - PALETTE_SIZE;

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int position = HEADER_SIZE;
		for (int y = 0; y < height; y++) {
			int x = 0;
			while (x < bytesPerLine) {
				int value = data[position++] & 0xFF;
				int count = 1;
				if ((value & 0xC0) == 0xC0) {
					count = value & 0x3F;
					value = data[position++] & 0xFF;
				}
				for (int i = 0; i < count; i++, x++) {
					if (x < width) {
						image.setRGB(x, y, paletteColor(data, paletteOffset, value));
					}
				}
			}
		}
		return image;
	}

	private static int paletteColor(byte[] data, int paletteOffset, int index) {
		int entry = paletteOffset + index * 3;
		int r = data[entry] & 0xFF;
		int g = data[entry + 1] & 0xFF;
		int b = data[entry + 2] & 0xFF;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int readShort(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
	}
}
