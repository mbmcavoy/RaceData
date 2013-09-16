package com.autosportLabs.data;

public abstract class DataChannelold {
	
	protected String name;
	protected String units;
	
	protected int valueByteOffset;
	
	public DataChannelold(String name, String units) {
		this.name = name;
		this.units = units;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getUnits() {
		return this.units;
	}
	

	
}
