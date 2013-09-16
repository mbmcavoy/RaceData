package com.autosportLabs.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("rawtypes")	// DataChannel is generic, but concrete classes identify the data type.
public class DataManager {

	// Data store
	private ArrayList<ByteBuffer> dataFrames = new ArrayList<ByteBuffer>();
	
	// Configuration Information
	private String configurationJSON;			// The Configuration Definition, in JSON format.
	private int framePeriod;					// The time period (in milliseconds) of each Data Frame.
	private int frameBytes;						// The number of bytes used per Frame.
	
	private Dictionary<String, DataChannelGroup> dataChannelGroups;
	private Dictionary<String, DataChannel> dataChannels;
	
	private RandomAccessFile storageFile;		// an associated data storage file.
	private long storageFileDataOffset;			// The offset for the start of binary data
												// (after the JSON configuration definition)
	
	public DataManager(String configurationJSON) throws Exception {
		this.configurationJSON = configurationJSON;
		
		initializeDataStreamConfiguration();
	}
	
	public DataManager(RandomAccessFile dataFile) throws Exception {
		this.storageFile = dataFile;
		this.storageFile.seek(0);	// ensure at position 0
		
		this.configurationJSON = this.storageFile.readUTF();
		this.storageFileDataOffset = this.storageFile.getFilePointer();
		
		initializeDataStreamConfiguration();
		
		//Load data into frames
		int frameCount = (int) ((this.storageFile.length() - this.storageFileDataOffset) / this.frameBytes);
		
		for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
			ByteBuffer frame = this.getFrameBytes(frameIndex);
			if (frame.hasArray()) {
				byte[] bytes = frame.array();
				this.storageFile.read(bytes);
			} else {
				throw new Exception("Unable to get Frame as array");
			}
		}
	}
	
	/**
	 * Parse the JSON definition to build up the data stream configuration. 
	 * @throws Exception 
	 */
	
	private void initializeDataStreamConfiguration() throws Exception {
		// Load the JSON data
		
		dataChannelGroups = new Hashtable<String, DataChannelGroup>();
		dataChannels = new Hashtable<String, DataChannel>();
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode configRootNode = mapper.readValue(this.configurationJSON, JsonNode.class);
			this.framePeriod = configRootNode.path("framePeriod").asInt();
			this.frameBytes = configRootNode.path("frameBytes").asInt();
			
			// Load Data Channel Groups
			JsonNode groupNodes = configRootNode.path("dataChannelGroups");
			
			// Process the Channel Groups
			for (JsonNode groupNode: groupNodes) {
				String groupName = groupNode.path("groupName").asText();
				int groupRate = groupNode.path("groupSampleRate").asInt();
				int groupOffset = groupNode.path("groupOffset").asInt();
				int groupSampleBytes = groupNode.path("groupSampleBytes").asInt();
	
				DataChannelGroup newGroup = new DataChannelGroup(groupName, groupRate, groupOffset, groupSampleBytes, this);
				
				JsonNode channelNodes = groupNode.path("groupChannels");
				
				for (JsonNode channelNode : channelNodes) {
					String channelType = channelNode.path("channelType").asText();
					String channelName = channelNode.path("channelName").asText();
					String channelUnits = channelNode.path("channelUnits").asText();
					int channelByteOffset = channelNode.path("channelByteOffset").asInt();
					
					DataChannel newChannel; 
					
					if (channelType.equals("analog")) {
						newChannel = new AnalogDataChannel(channelName,
								channelUnits,
								channelByteOffset,
								this,
								newGroup);
					}
					// handle other types here!
					else {
						throw new Exception("Unknown Channel type: " + channelType);
					}
					 this.dataChannels.put(channelName,	newChannel);
					
				}
				
				this.dataChannelGroups.put(groupName, newGroup);
			}
			
			
		
		}
		catch (Exception e) {
			throw new Exception("Unable to read configuration definition.", e.getCause());
		}
	}

	public void setNewDataFile(RandomAccessFile file) throws IOException {
		this.storageFile = file;
		this.storageFile.seek(0);	// ensure at position 0
		this.storageFile.writeUTF(this.configurationJSON);
		this.storageFileDataOffset = this.storageFile.getFilePointer();
	}
	
	public void writeFrame(int frameIndex) throws Exception {
		long position = this.storageFileDataOffset + (this.frameBytes * frameIndex);
		this.storageFile.seek(position);
		ByteBuffer frame = this.dataFrames.get(frameIndex); 
		
		if ( frame.hasArray() ) {
			byte[] bytes = frame.array();
			this.storageFile.write(bytes);
		} else {
			throw new Exception("Unable to get Frame as array");
		}
	}
	
	/**
	 * @return the Frame Period (in milliseconds)
	 */
	public int getFramePeriod() {
		return this.framePeriod;
	}
	
	/**
	 * @return the Configuration definition, in JSON format
	 */
	public String getConfigurationJSON() {
		return configurationJSON;
	}
	
	/**
	 * Gets the DataChannelGroup with the matching name.
	 * @param groupName The name of the DataChannelGroup
	 * @return The named DataChannelGroup; null if the name does not exist.
	 */
	public DataChannelGroup getDataChannelGroup(String groupName) {
		return dataChannelGroups.get(groupName);				
	}
	
	
	/**
	 * Gets the DataChannel with the matching name.
	 * @param channelName The name of the DataChannel
	 * @return The named DataChannel; null if the name does not exist.
	 */
	public DataChannel getDataChannel(String channelName) {
		return dataChannels.get(channelName);
	}
	
	ByteBuffer getFrameBytes(int frameIndex) {
		extendFramesToIndex(frameIndex);
		return this.dataFrames.get(frameIndex);
	}
	
	private void extendFramesToIndex(int frameIndex) {
		if (frameIndex >= this.dataFrames.size()) {
			for (int index = this.dataFrames.size() ; index <= frameIndex; index++) {
				ByteBuffer newFrame = ByteBuffer.allocate(this.frameBytes);
				this.dataFrames.add(newFrame);
			}
		}
	}
	
}
