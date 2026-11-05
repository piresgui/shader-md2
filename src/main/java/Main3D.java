
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import md2.Md2Animator;
import md2.Md2File;
import md2.Md2Model;
import shaders.Md2Shader;
import util.PcxLoader;
import util.TextureLoader;

import java.awt.image.BufferedImage;

//import com.sun.org.apache.xerces.internal.dom.DeepNodeListImpl;

import java.io.File;
import java.io.IOException;
import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main3D {

	private static final String DEFAULT_MODEL = "models/ogro/ogro.md2";
	private static final String[] SKIN_EXTENSIONS = { ".pcx", ".png", ".jpg" };

	// The window handle
	private long window;

	float viewAngX = 15;
	float viewAngY = -90;
	float scale = 1.0f;

	boolean interpolate = true;

	String modelPath;
	Md2Model md2;
	Md2Animator animator;
	Md2Shader shader;

	public Main3D(String modelPath) {
		this.modelPath = modelPath;
	}

	public void run() throws IOException {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(1500, 1000, "Hello World!", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated
		// or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop

			if ( key == GLFW_KEY_W) {
				viewAngX+=5;
			}
			if ( key == GLFW_KEY_S) {
				viewAngX-=5;
			}
			if ( key == GLFW_KEY_A) {
				viewAngY+=5;
			}
			if ( key == GLFW_KEY_D) {
				viewAngY-=5;
			}
			if ( key == GLFW_KEY_Z) {
				scale = scale*1.1f;
			}
			if ( key == GLFW_KEY_X) {
				scale = scale*0.9f;
			}
			if (action == GLFW_PRESS) {
				if ( key == GLFW_KEY_RIGHT) {
					animator.changeAnimation(1);
				}
				if ( key == GLFW_KEY_LEFT) {
					animator.changeAnimation(-1);
				}
				if ( key == GLFW_KEY_UP) {
					animator.framesPerSecond = Math.min(animator.framesPerSecond + 1, 30);
				}
				if ( key == GLFW_KEY_DOWN) {
					animator.framesPerSecond = Math.max(animator.framesPerSecond - 1, 1);
				}
				if ( key == GLFW_KEY_I) {
					interpolate = !interpolate;
					System.out.println("Interpolacao: " + interpolate);
				}
			}

		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void loop() throws IOException {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.

		GL.createCapabilities();

		shader = new Md2Shader();

		Md2File file = Md2File.read(modelPath);
		System.out.println("MD2 " + modelPath + " Frames: " + file.numFrames + " Triangulos: " + file.numTriangles);
		md2 = new Md2Model(file);
		md2.load();
		animator = new Md2Animator(file.frameNames);
		int texture = TextureLoader.loadTexture(loadSkin(file));

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.

		int frame = 0;
		long lasttime = System.currentTimeMillis();

		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glClearColor(0.15f, 0.17f, 0.22f, 1.0f);

		shader.start();
		int modellocation = glGetUniformLocation(shader.programID, "model");
		int viewlocation = glGetUniformLocation(shader.programID, "view");
		int projectionlocation = glGetUniformLocation(shader.programID, "projection");
		int interplocation = glGetUniformLocation(shader.programID, "interp");
		glUniform1i(glGetUniformLocation(shader.programID, "tex"), 0);
		glUniform3f(glGetUniformLocation(shader.programID, "lightDirection"), 0.4f, 0.7f, 0.6f);

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture);

		Matrix4f modelm = new Matrix4f();
		modelm.scale(new Vector3f(1f / md2.radius, 1f / md2.radius, 1f / md2.radius));
		modelm.translate(new Vector3f(-md2.centerX, -md2.centerY, -md2.centerZ));

		FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
		uploadMatrix(modellocation, modelm, matrixBuffer);

		long ultimoTempo = System.currentTimeMillis();
		while (!glfwWindowShouldClose(window)) {

			long diftime = System.currentTimeMillis()-ultimoTempo;
			ultimoTempo = System.currentTimeMillis();

			animator.update(diftime);

			int[] width = new int[1];
			int[] height = new int[1];
			glfwGetFramebufferSize(window, width, height);
			glViewport(0, 0, width[0], height[0]);

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			Matrix4f view = new Matrix4f();
			view.translate(new Vector3f(0,0,-3.2f));
			view.scale(new Vector3f(scale,scale,scale));
			view.rotate(viewAngX*0.0174532f, new Vector3f(1,0,0));
			view.rotate(viewAngY*0.0174532f, new Vector3f(0,1,0));

			float aspect = (float) width[0] / Math.max(height[0], 1);
			uploadMatrix(viewlocation, view, matrixBuffer);
			uploadMatrix(projectionlocation, perspective(45, aspect, 0.1f, 100), matrixBuffer);

			glUniform1f(interplocation, interpolate ? animator.interp : 0);

			md2.setFrames(animator.frameA, animator.frameB);
			md2.draw();

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
			frame++;
			long actualTime = System.currentTimeMillis();
			if ((lasttime / 1000) != (actualTime / 1000)) {
				System.out.println("FPS " + frame);
				frame = 0;
				lasttime = actualTime;
			}

		}

		shader.stop();
	}

	private BufferedImage loadSkin(Md2File file) throws IOException {
		if (file.skinNames.length == 0) {
			throw new IOException("MD2 sem skin: " + modelPath);
		}
		File folder = new File(modelPath).getParentFile();
		for (String skin : file.skinNames) {
			String baseName = new File(skin.replace('\\', '/')).getName().replaceAll("\\.[^.]*$", "");
			for (String extension : SKIN_EXTENSIONS) {
				File skinFile = new File(folder, baseName + extension);
				if (skinFile.exists()) {
					System.out.println("Skin: " + skinFile.getPath());
					if (extension.equals(".pcx")) {
						return PcxLoader.load(skinFile.getPath());
					}
					return TextureLoader.loadImage(skinFile.getPath());
				}
			}
		}
		throw new IOException("Nenhuma skin encontrada para: " + modelPath);
	}

	private void uploadMatrix(int location, Matrix4f matrix, FloatBuffer buffer) {
		buffer.clear();
		matrix.store(buffer);
		buffer.flip();
		glUniformMatrix4fv(location, false, buffer);
	}

	private Matrix4f perspective(float fovy, float aspect, float near, float far) {
		float f = 1f / (float) Math.tan(Math.toRadians(fovy) / 2);
		Matrix4f projection = new Matrix4f();
		projection.m00 = f / aspect;
		projection.m11 = f;
		projection.m22 = (far + near) / (near - far);
		projection.m23 = -1;
		projection.m32 = 2 * far * near / (near - far);
		projection.m33 = 0;
		return projection;
	}


	public static void main(String[] args) throws IOException {
		new Main3D(args.length > 0 ? args[0] : DEFAULT_MODEL).run();
	}

}
