/**
 * 
 */
package com.autosportLabs.data;

import java.nio.ByteBuffer;

/**
 * @author Mike
 *
 */
public abstract class DataChannel<T> {
	
	protected final String name;
	protected final String units;
	protected final int byteOffset;
	protected final DataManager manager;
	protected final DataChannelGroup group;
	
	
	
	public DataChannel(String name, 
			String units,
			int byteOffset,
			DataManager manager,
			DataChannelGroup group) {
		this.name = name;
		this.units = units;
		
		this.byteOffset = byteOffset;
		
		this.manager = manager;
		this.group = group;
	}
	
	/**
	 * Gets the name of the Data Channel.
	 * @return the name of the Data Channel
	 */
	public final String getName() {
		return this.name;
	}
	
	/**
	 * Gets the units for the Data Channel 
	 * @return the units for the Data Channel
	 */
	public final String getUnits() {
		return this.units;
	}

	/**
	 * Sets the value for for the frame and sample indexes.
	 * @param value the value to set
	 * @param frameIndex the frame index to contain the value
	 * @param sampleIndex the sample index to contain the value
	 */
	public final void setValue(T value, int frameIndex, int sampleIndex) {
		// Determine the location reserved for this DataChannelGroup sample set
		int offset = this.byteOffset + (sampleIndex * this.group.getSampleBytes());
				
		// Get the data from the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(offset);

		insertValueIntoFrame(value, frame);
	}
	
	public final T getValue(int frameIndex, int sampleIndex) {
		// Determine the location reserved for this DataChannelGroup sample set
		int offset = this.byteOffset + (sampleIndex * this.group.getSampleBytes());
		
		// Get the data from the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(offset);

		return extractValueFromFrame(frame);
	}
	
	protected abstract void insertValueIntoFrame(T value, ByteBuffer frame);
	
	protected abstract T extractValueFromFrame(ByteBuffer frame);
	
	public final T getValue(int index) {
		int frameIndex = index / this.group.getSampleRate();
		int sampleIndex = index % this.group.getSampleRate();
		return this.getValue(frameIndex, sampleIndex);
	}
	
	public final void setValue(T value, int index) {
		int frameIndex = index / this.group.getSampleRate();
		int sampleIndex = index % this.group.getSampleRate();
		this.setValue(value, frameIndex, sampleIndex);
	}
	
	public boolean isValid(int frameIndex, int sampleIndex) {
		return false;
	}
	
	public final boolean isValid(int index) {
		int frameIndex = index / this.group.getSampleRate();
		int sampleIndex = index % this.group.getSampleRate();
		return this.isValid(frameIndex, sampleIndex);
	}
	
}
