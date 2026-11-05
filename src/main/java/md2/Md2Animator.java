package md2;

import java.util.ArrayList;
import java.util.List;

public class Md2Animator {
	private static class Animation {
		final String name;
		final int firstFrame;
		int frameCount = 1;

		Animation(String name, int firstFrame) {
			this.name = name;
			this.firstFrame = firstFrame;
		}
	}

	private final List<Animation> animations = new ArrayList<>();
	private int current = 0;
	private float time = 0;

	public float framesPerSecond = 8;
	public int frameA;
	public int frameB;
	public float interp;

	public Md2Animator(String[] frameNames) {
		Animation last = null;
		for (int i = 0; i < frameNames.length; i++) {
			String name = frameNames[i].replaceAll("[0-9_]+$", "");
			if (last != null && last.name.equals(name)) {
				last.frameCount++;
			} else {
				last = new Animation(name, i);
				animations.add(last);
			}
		}
		update(0);
	}

	public void update(long elapsedMillis) {
		Animation animation = animations.get(current);
		time = (time + elapsedMillis * framesPerSecond / 1000f) % animation.frameCount;
		int index = (int) time;
		frameA = animation.firstFrame + index;
		frameB = animation.firstFrame + (index + 1) % animation.frameCount;
		interp = time - index;
	}

	public void changeAnimation(int direction) {
		current = (current + direction + animations.size()) % animations.size();
		time = 0;
		System.out.println("Animacao: " + animations.get(current).name);
	}
}
