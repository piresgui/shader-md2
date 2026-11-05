package md2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Md2File {
	private static final int IDENT = 0x32504449;
	private static final int VERSION = 8;
	private static final int SKIN_NAME_SIZE = 64;
	private static final int FRAME_NAME_SIZE = 16;

	public int skinWidth;
	public int skinHeight;
	public int numVertices;
	public int numTriangles;
	public int numFrames;

	public String[] skinNames;
	public short[] texS;
	public short[] texT;
	public short[][] triangleVertices;
	public short[][] triangleTexCoords;
	public String[] frameNames;
	public float[][] framePositions;

	public static Md2File read(String path) throws IOException {
		ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(Paths.get(path))).order(ByteOrder.LITTLE_ENDIAN);

		int ident = data.getInt();
		int version = data.getInt();
		if (ident != IDENT || version != VERSION) {
			throw new IOException("Arquivo MD2 invalido: " + path);
		}

		Md2File md2 = new Md2File();
		md2.skinWidth = data.getInt();
		md2.skinHeight = data.getInt();
		int frameSize = data.getInt();
		int numSkins = data.getInt();
		md2.numVertices = data.getInt();
		int numTexCoords = data.getInt();
		md2.numTriangles = data.getInt();
		data.getInt();
		md2.numFrames = data.getInt();
		int ofsSkins = data.getInt();
		int ofsTexCoords = data.getInt();
		int ofsTriangles = data.getInt();
		int ofsFrames = data.getInt();

		md2.readSkins(data, ofsSkins, numSkins);
		md2.readTexCoords(data, ofsTexCoords, numTexCoords);
		md2.readTriangles(data, ofsTriangles);
		md2.readFrames(data, ofsFrames, frameSize);
		return md2;
	}

	private void readSkins(ByteBuffer data, int offset, int numSkins) {
		skinNames = new String[numSkins];
		for (int i = 0; i < numSkins; i++) {
			data.position(offset + i * SKIN_NAME_SIZE);
			skinNames[i] = readString(data, SKIN_NAME_SIZE);
		}
	}

	private void readTexCoords(ByteBuffer data, int offset, int numTexCoords) {
		texS = new short[numTexCoords];
		texT = new short[numTexCoords];
		data.position(offset);
		for (int i = 0; i < numTexCoords; i++) {
			texS[i] = data.getShort();
			texT[i] = data.getShort();
		}
	}

	private void readTriangles(ByteBuffer data, int offset) {
		triangleVertices = new short[numTriangles][3];
		triangleTexCoords = new short[numTriangles][3];
		data.position(offset);
		for (int i = 0; i < numTriangles; i++) {
			for (int j = 0; j < 3; j++) {
				triangleVertices[i][j] = data.getShort();
			}
			for (int j = 0; j < 3; j++) {
				triangleTexCoords[i][j] = data.getShort();
			}
		}
	}

	private void readFrames(ByteBuffer data, int offset, int frameSize) {
		frameNames = new String[numFrames];
		framePositions = new float[numFrames][numVertices * 3];
		for (int f = 0; f < numFrames; f++) {
			data.position(offset + f * frameSize);
			float scaleX = data.getFloat();
			float scaleY = data.getFloat();
			float scaleZ = data.getFloat();
			float translateX = data.getFloat();
			float translateY = data.getFloat();
			float translateZ = data.getFloat();
			frameNames[f] = readString(data, FRAME_NAME_SIZE);

			for (int v = 0; v < numVertices; v++) {
				float x = (data.get() & 0xFF) * scaleX + translateX;
				float y = (data.get() & 0xFF) * scaleY + translateY;
				float z = (data.get() & 0xFF) * scaleZ + translateZ;
				data.get();
				framePositions[f][v * 3] = x;
				framePositions[f][v * 3 + 1] = z;
				framePositions[f][v * 3 + 2] = -y;
			}
		}
	}

	private static String readString(ByteBuffer data, int size) {
		byte[] raw = new byte[size];
		data.get(raw);
		int length = 0;
		while (length < size && raw[length] != 0) {
			length++;
		}
		return new String(raw, 0, length, StandardCharsets.US_ASCII);
	}
}
