/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.murillo.sfu.utils;

import java.util.Vector;

/**
 *
 * http://en.wikipedia.org/wiki/Linear_feedback_shift_register
 * 
 */
public class PSNRSequence {

	private static final int M = 15;

	// hard-coded for 15-bits
	private static final int[] TAPS = {14, 15};

	private final boolean[] bits = new boolean[M + 1];

	public PSNRSequence() {
		this((int) System.currentTimeMillis());
	}

	public PSNRSequence(int seed) {
		for (int i = 0; i < M; i++) {
			bits[i] = (((1 << i) & seed) >>> i) == 1;
		}
	}

	/* generate a random int uniformly on the interval [-2^31 + 1, 2^31 - 1] */
	public short nextShort() {

		// calculate the integer value from the registers
		short next = 0;
		for (int i = 0; i < M; i++) {
			next |= (bits[i] ? 1 : 0) << i;
		}

		// allow for zero without allowing for -2^31
		if (next < 0) {
			next++;
		}

		// calculate the last register from all the preceding
		bits[M] = false;
		for (int i = 0; i < TAPS.length; i++) {
			bits[M] ^= bits[M - TAPS[i]];
		}

		// shift all the registers
		for (int i = 0; i < M; i++) {
			bits[i] = bits[i + 1];
		}

		return next;
	}

	/**
	 * returns random double uniformly over [0, 1)
	 */
	public double nextDouble() {
		return ((nextShort() / (Integer.MAX_VALUE + 1.0)) + 1.0) / 2.0;
	}

	/**
	 * returns random boolean
	 */
	public boolean nextBoolean() {
		return nextShort() >= 0;
	}

	public void printBits() {
		System.out.print(bits[M] ? 1 : 0);
		System.out.print(" -> ");
		for (int i = M - 1; i >= 0; i--) {
			System.out.print(bits[i] ? 1 : 0);
		}
		System.out.println();
	}

	public static void main(String[] args) {
		PSNRSequence rng = new PSNRSequence();
		Vector<Short> vec = new Vector<Short>();
		for (int i = 0; i <= 32766; i++) {
			short next = rng.nextShort();
			// just testing/asserting to make 
			// sure the number doesn't repeat on a given list
			if (vec.contains(next)) {
				throw new RuntimeException("Index repeat: " + i);
			}
			vec.add(next);
			System.out.println(next);
		}
	}
}
