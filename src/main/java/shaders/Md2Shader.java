package shaders;

public class Md2Shader extends ShaderProgram {

	private static final String VERTEX_FILE = "./shaders/md2VertexShader.txt";
	private static final String FRAGMENT_FILE = "./shaders/md2FragmentShader.txt";

	public Md2Shader() {
		super(VERTEX_FILE, FRAGMENT_FILE);
	}

	@Override
	protected void bindAttributes() {
		super.bindAttribute(0, "positionA");
		super.bindAttribute(1, "positionB");
		super.bindAttribute(2, "normalA");
		super.bindAttribute(3, "normalB");
		super.bindAttribute(4, "textuv");
	}
}
