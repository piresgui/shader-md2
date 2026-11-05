package md2;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import Model.Model;

public class Md2Model extends Model {
	private static final int ATTR_POSITION_A = 0;
	private static final int ATTR_POSITION_B = 1;
	private static final int ATTR_NORMAL_A = 2;
	private static final int ATTR_NORMAL_B = 3;
	private static final int ATTR_TEX_COORD = 4;
	private static final int FLOATS_PER_VERTEX = 6;
	private static final int STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

	public final Md2File file;
	public float centerX;
	public float centerY;
	public float centerZ;
	public float radius;

	private final int vertexCount;
	private int vao;
	private int frameVbo;
	private int frameA;
	private int frameB;

	public Md2Model(Md2File file) {
		this.file = file;
		this.vertexCount = file.numTriangles * 3;
		computeBounds();
	}

	public void setFrames(int frameA, int frameB) {
		this.frameA = frameA;
		this.frameB = frameB;
	}

	@Override
	public void load() {
		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		frameVbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, frameVbo);
		glBufferData(GL_ARRAY_BUFFER, buildFrameData(), GL_STATIC_DRAW);

		int texCoordVbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, texCoordVbo);
		glBufferData(GL_ARRAY_BUFFER, buildTexCoordData(), GL_STATIC_DRAW);
		glVertexAttribPointer(ATTR_TEX_COORD, 2, GL_FLOAT, false, 0, 0);

		glEnableVertexAttribArray(ATTR_POSITION_A);
		glEnableVertexAttribArray(ATTR_POSITION_B);
		glEnableVertexAttribArray(ATTR_NORMAL_A);
		glEnableVertexAttribArray(ATTR_NORMAL_B);
		glEnableVertexAttribArray(ATTR_TEX_COORD);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	@Override
	public void draw() {
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, frameVbo);
		bindFrame(frameA, ATTR_POSITION_A, ATTR_NORMAL_A);
		bindFrame(frameB, ATTR_POSITION_B, ATTR_NORMAL_B);
		glDrawArrays(GL_TRIANGLES, 0, vertexCount);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	private void bindFrame(int frame, int positionAttribute, int normalAttribute) {
		long offset = (long) frame * vertexCount * STRIDE;
		glVertexAttribPointer(positionAttribute, 3, GL_FLOAT, false, STRIDE, offset);
		glVertexAttribPointer(normalAttribute, 3, GL_FLOAT, false, STRIDE, offset + 3 * Float.BYTES);
	}

	private FloatBuffer buildFrameData() {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(file.numFrames * vertexCount * FLOATS_PER_VERTEX);
		for (int f = 0; f < file.numFrames; f++) {
			float[] positions = file.framePositions[f];
			float[] normals = computeNormals(positions);
			for (int t = 0; t < file.numTriangles; t++) {
				short[] triangle = file.triangleVertices[t];
				putVertex(buffer, positions, normals, triangle[0]);
				putVertex(buffer, positions, normals, triangle[2]);
				putVertex(buffer, positions, normals, triangle[1]);
			}
		}
		buffer.flip();
		return buffer;
	}

	private FloatBuffer buildTexCoordData() {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexCount * 2);
		for (int t = 0; t < file.numTriangles; t++) {
			short[] triangle = file.triangleTexCoords[t];
			putTexCoord(buffer, triangle[0]);
			putTexCoord(buffer, triangle[2]);
			putTexCoord(buffer, triangle[1]);
		}
		buffer.flip();
		return buffer;
	}

	private void putVertex(FloatBuffer buffer, float[] positions, float[] normals, int vertex) {
		buffer.put(positions, vertex * 3, 3);
		buffer.put(normals, vertex * 3, 3);
	}

	private void putTexCoord(FloatBuffer buffer, int texCoord) {
		buffer.put((float) file.texS[texCoord] / file.skinWidth);
		buffer.put((float) file.texT[texCoord] / file.skinHeight);
	}

	private float[] computeNormals(float[] p) {
		float[] normals = new float[p.length];
		for (int t = 0; t < file.numTriangles; t++) {
			short[] triangle = file.triangleVertices[t];
			int a = triangle[0] * 3;
			int b = triangle[2] * 3;
			int c = triangle[1] * 3;
			float ux = p[b] - p[a];
			float uy = p[b + 1] - p[a + 1];
			float uz = p[b + 2] - p[a + 2];
			float vx = p[c] - p[a];
			float vy = p[c + 1] - p[a + 1];
			float vz = p[c + 2] - p[a + 2];
			float nx = uy * vz - uz * vy;
			float ny = uz * vx - ux * vz;
			float nz = ux * vy - uy * vx;
			for (int index : new int[] { a, b, c }) {
				normals[index] += nx;
				normals[index + 1] += ny;
				normals[index + 2] += nz;
			}
		}
		for (int i = 0; i < normals.length; i += 3) {
			float length = (float) Math.sqrt(normals[i] * normals[i] + normals[i + 1] * normals[i + 1] + normals[i + 2] * normals[i + 2]);
			if (length > 0) {
				normals[i] /= length;
				normals[i + 1] /= length;
				normals[i + 2] /= length;
			}
		}
		return normals;
	}

	private void computeBounds() {
		float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		for (float[] positions : file.framePositions) {
			for (int i = 0; i < positions.length; i++) {
				min[i % 3] = Math.min(min[i % 3], positions[i]);
				max[i % 3] = Math.max(max[i % 3], positions[i]);
			}
		}
		centerX = (min[0] + max[0]) / 2;
		centerY = (min[1] + max[1]) / 2;
		centerZ = (min[2] + max[2]) / 2;
		radius = Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2])) / 2;
	}
}
