package io.github.razekteixeira.roofwright.core;

/** A roof that cannot be planned, with a message fit for chat. */
public class PlanException extends Exception {
	public PlanException(String message) {
		super(message);
	}
}
