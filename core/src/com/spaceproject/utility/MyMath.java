package com.spaceproject.utility;

import com.spaceproject.SpaceProject;

public class MyMath {
	
	public static long getSeed(float x, float y) {
		return getSeed((int)x, (int)y);
	}
	
	public static long getSeed(int x, int y) {
		//long is 64 bits. store x in upper bits, y in lower bits
		return (x << 32) + y + SpaceProject.SEED;
	}

	/**
	 * Get distance from position 1 to position 2.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return distance
	 */
	public static float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.hypot(x2 - x1, y2 - y1);
	}
	
	/**
	 * Get angle from position 1 to position 2.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return angle
	 */
	public static float angleTo(int x1, int y1, int x2, int y2) {
		return (float) -(Math.atan2(x2 - x1, y2 - y1)) - 1.57f;
	}
	
	/**
	 * Round value with specified precision.
	 * @param value
	 * @param precision number of decimal points
	 * @return rounded value
	 */
	public static double round(double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}
	
	/**
	 * Returns the percentage of the range max-min that corresponds to value.
	 * @param min
	 * @param max
	 * @param value
	 * @return interpolant value within the range [min, max].
	 */
	public static float inverseLerp(float min, float max, float value) {
		return (value - min) / (max - min);
	}
}
