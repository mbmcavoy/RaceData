package com.autosportLabs.data;

import java.nio.ByteBuffer;

public final class BooleanDataChannel extends DataChannel<Boolean> {
	
	// The Boolean value only uses one bit, so we must also know about which bit.
	private final int bitOffset;
	private final byte trueValue;
	private final byte mask;

	public BooleanDataChannel(String name,
			String units,
			int byteOffset,
			int bitOffset,
			DataManager manager,
			DataChannelGroup group) {
		super(name, units, byteOffset, manager, group);
		this.bitOffset = bitOffset;
		this.trueValue = (byte)(0x01 << this.bitOffset);
		this.mask = (byte)(~trueValue);
	}

	@Override
	protected void insertValueIntoFrame(Boolean value, ByteBuffer frame) {
		byte previousByte = frame.get(); // side effect: advances the frame's position by one byte.
		
		// Set or clear our bit, while keeping the remaining bits unchanged.
		byte newByte;
		if (value) {
			newByte = (byte)(previousByte | this.trueValue);
		} else {
			newByte = (byte)(previousByte & this.mask);
		}
		
		// steps the frame back to the position upon entry.
		frame.position(frame.position() - 1);
		frame.put(newByte);
	}

	@Override
	protected Boolean extractValueFromFrame(ByteBuffer frame) {
		byte containingByte = frame.get();
		
		// isolate our bit
		byte isolatedBit = (byte) ((containingByte >> bitOffset) & 0x01);
		
		// compare
		return (isolatedBit == 0x01);
	}




}
