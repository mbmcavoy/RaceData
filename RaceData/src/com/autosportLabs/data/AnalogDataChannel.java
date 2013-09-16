/**
 * 
 */
package com.autosportLabs.data;

import java.nio.ByteBuffer;

/**
 * @author Mike
 *
 */
public final class AnalogDataChannel extends DataChannel<Float> {

	public AnalogDataChannel(String name, String units, int byteOffset,
			DataManager manager, DataChannelGroup group) {
		super(name, units, byteOffset, manager, group);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void insertValueIntoFrame(Float value, ByteBuffer frame) {
		frame.putFloat(value);
	}

	@Override
	protected Float extractValueFromFrame(ByteBuffer frame) {
		return frame.getFloat();
	}






}
