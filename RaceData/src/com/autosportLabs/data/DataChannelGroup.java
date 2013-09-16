package com.autosportLabs.data;

import java.nio.ByteBuffer;

public class DataChannelGroup {

	private final String name;
	private final int sampleRate;
	private final int byteOffset;
	private final int sampleBytes;
	private final DataManager manager;
	
	private final byte validBit = 0x01;
	private final byte validBitMask = (byte) (~validBit);
		
	public DataChannelGroup(String name, int sampleRate, int byteOffset, int sampleBytes, DataManager manager) {
		this.name = name;
		this.sampleRate = sampleRate;
		this.byteOffset = byteOffset;
		this.sampleBytes = sampleBytes;
		this.manager = manager;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getSampleRate() {
		return this.sampleRate;
	}
	
	public int getSampleBytes() {
		return this.sampleBytes;
	}
	
	public void setBytesForFrame(byte[] bytes, int frameIndex) throws Exception {
		// Check to ensure the data received is the correct length
		int expectedBytes = this.sampleBytes * this.sampleRate;
		if (expectedBytes != bytes.length) {
			String message = String.format("Byte length mismatch. Expected %s, received %s", expectedBytes, bytes.length);
			throw new Exception(message);
		}	
		
		// Put the data in the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(this.byteOffset);
		frame.put(bytes);
	}
	
	public byte[] getBytesForFrame(int frameIndex) {
		// Prepare the output array
		int byteCount = this.sampleBytes * this.sampleRate;
		byte[] output = new byte[byteCount];
		
		// Get the data from the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(this.byteOffset);
		frame.get(output);		// transfers the data into the output array
		
		return output;
	}
	
	public void setBytesForSample(byte[] bytes, int frameIndex, int sampleIndex) throws Exception {
		// Check to ensure the data received is the correct length 
		if (this.sampleBytes != bytes.length) {
			String message = String.format("Byte length mismatch. Expected %s, received %s", this.sampleBytes, bytes.length);
			throw new Exception(message);
		}
		
		// Determine the location reserved for this DataChannelGroup sample set
		int offset = this.byteOffset + (sampleIndex * this.sampleBytes);
		
		// Put the data in the frame at the location reserved
		ByteBuffer frame = manager.getFrameBytes(frameIndex);
		frame.position(offset);
		frame.put(bytes);
	}
	
	public void setBytesForSample(byte[] bytes, int index) throws Exception {
		int frameIndex = index / this.getSampleRate();
		int sampleIndex = index % this.getSampleRate();
		this.setBytesForSample(bytes, frameIndex, sampleIndex);
	}

	public byte[] getBytesForSample(int frameIndex, int sampleIndex) {
		// Prepare the output array
		byte[] output = new byte[this.sampleBytes];
		
		// Determine the location reserved for this DataChannelGroup sample set
		int offset = this.byteOffset + (sampleIndex * this.sampleBytes);
		
		// Get the data from the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(offset);
		frame.get(output);		// transfers the data into the output array
		
		return output;
	}
	
	public byte[] getBytesForSample(int index) {
		int frameIndex = index / this.getSampleRate();
		int sampleIndex = index % this.getSampleRate();
		return this.getBytesForSample(frameIndex, sampleIndex);
	}

	public void setValid(boolean isValid, int frameIndex, int sampleIndex) {	
		// Determine the location reserved for this DataChannelGroup sample set
		// Validity for sample set is Byte 0, Bit 0.
		int offset = this.byteOffset + (sampleIndex * this.sampleBytes);
		
		// Get the data from the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(offset);
		byte containingByte = frame.get();
		
		// Set or Clear the validity bit 
		byte newByte;
		if (isValid) {
			newByte = (byte) (containingByte | validBit);
		} else {
			newByte = (byte) (containingByte & validBitMask);
		}
		
		// Store the value back into the frame
		frame.position(offset);
		frame.put(newByte);
	}
	
	public void setValid(boolean isValid, int index) {
		int frameIndex = index / this.getSampleRate();
		int sampleIndex = index % this.getSampleRate();
		this.setValid(isValid, frameIndex, sampleIndex);
	}
	
	public boolean isValid (int frameIndex, int sampleIndex) {
		// Determine the location reserved for this DataChannelGroup sample set
		// Validity for sample set is Byte 0, Bit 0.
		int offset = this.byteOffset + (sampleIndex * this.sampleBytes);
		
		// Get the data from the frame at the location reserved for this DataChannelGroup
		ByteBuffer frame = this.manager.getFrameBytes(frameIndex);
		frame.position(offset);
		byte containingByte = frame.get();

		return ((containingByte & validBit) == validBit);
	}
	
	public boolean isValid(int index) {
		int frameIndex = index / this.getSampleRate();
		int sampleIndex = index % this.getSampleRate();
		return this.isValid(frameIndex, sampleIndex);
	}
	
}
