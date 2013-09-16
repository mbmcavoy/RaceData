package com.autosportLabs.captureMock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.autosportLabs.data.AnalogDataChannel;
import com.autosportLabs.data.DataChannelGroup;
import com.autosportLabs.data.DataManager;

public class CaptureMock {
	
	public static void main(String[] args) throws Exception {
		
		String originalDataFile = "rc_0.log";
		
		
		byte[] encodedJSON = Files.readAllBytes(Paths.get("config.json"));
		String stringJSON = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encodedJSON)).toString();
		
		DataManager captureDataManager = new DataManager(stringJSON);
		
		DataManager telemetryDataManager = simulateCaptureAndTelemetry(originalDataFile, captureDataManager);
		
		 // Check correctness of telemetry data
		 System.out.println("Checking telemetry data received");
		 checkData(originalDataFile, telemetryDataManager);
		 System.out.println();
		 
		 // TODO:  reload, check.
		 RandomAccessFile loadFile = new RandomAccessFile("capture.rcap", "r");
		 DataManager loadedDataManager = new DataManager(loadFile);
		 
		 System.out.println("Checking stored data loaded");
		 checkData(originalDataFile, loadedDataManager);
		 
		 
		
	}
	
	 static DataManager simulateCaptureAndTelemetry(String fileName, DataManager captureDataManager) throws Exception {
		 BufferedReader br = new BufferedReader(new FileReader(fileName));
		 int rowNumber = 0;
		 String line = br.readLine(); // get past the header line, no processing.
		 
		 AnalogDataChannel inertialChannelX = (AnalogDataChannel) captureDataManager.getDataChannel("Lateral Acceleration");
		 AnalogDataChannel inertialChannelY = (AnalogDataChannel) captureDataManager.getDataChannel("Longitudinal Acceleration");
		 AnalogDataChannel inertialChannelZ = (AnalogDataChannel) captureDataManager.getDataChannel("Vertical Acceleration");
		 AnalogDataChannel inertialChannelYaw = (AnalogDataChannel) captureDataManager.getDataChannel("Yaw Rate");		 
		 
		 DataChannelGroup captureInertialGroup = captureDataManager.getDataChannelGroup("Inertial Measurements");
		 
		 //Initialize Telemetry
		 String telemetryJSON = captureDataManager.getConfigurationJSON();
		 DataManager telemetryDataManager = new DataManager(telemetryJSON);
		 DataChannelGroup telemetryInertialGroup = telemetryDataManager.getDataChannelGroup("Inertial Measurements");
		 
		 // delete the file if it already exists
		 File checkFile = new File("capture.rcap");
		 if (checkFile.exists()) {
			 checkFile.delete();
		 }
		 
		 
		 RandomAccessFile captureFile = new RandomAccessFile("capture.rcap", "rws");
		 captureDataManager.setNewDataFile(captureFile);
		 
		 
		 while ((line = br.readLine()) != null) {
			 String[] columns = line.split(",");
			 
			 Float inertialValueX = Float.valueOf(columns[0]);
			 Float inertialValueY = Float.valueOf(columns[1]);
			 Float inertialValueZ = Float.valueOf(columns[2]);
			 Float inertialValueYaw = Float.valueOf(columns[3]);
			 
			 inertialChannelX.setValue(inertialValueX, rowNumber);
			 inertialChannelY.setValue(inertialValueY, rowNumber);
			 inertialChannelZ.setValue(inertialValueZ, rowNumber);
			 inertialChannelYaw.setValue(inertialValueYaw, rowNumber);
			 
			 captureInertialGroup.setValid(true, rowNumber);
			 
			 // TODO: Process GPS data at a different rate.
			 
			 // Simulate normal Telemetry data cycle
			 if ((rowNumber % 10) == 0) {	// 3 Hz Updates
				 byte[] transmittedSampleBytes = captureInertialGroup.getBytesForSample(rowNumber);
				 telemetryInertialGroup.setBytesForSample(transmittedSampleBytes, rowNumber);
			 }
			 
			 // TODO: Simulate detailed Telemetry requests 
			 
			 rowNumber++;
			 
			 // Commit when done with a frame
			 if (rowNumber % captureInertialGroup.getSampleRate() == 0) {
				 captureDataManager.writeFrame((rowNumber / captureInertialGroup.getSampleRate()) - 1);
			 }
		 }
		 
		 // Commit last data
		 captureDataManager.writeFrame(rowNumber / captureInertialGroup.getSampleRate());
		 
		 // close files
		 captureFile.close();
		 br.close();
		 return telemetryDataManager;
	}

	private static void checkData(String originalFileName, DataManager manager)
			throws FileNotFoundException, IOException {
		BufferedReader br;
		int rowNumber;
		String line;

		 AnalogDataChannel receivedX = (AnalogDataChannel) manager.getDataChannel("Lateral Acceleration");
		 AnalogDataChannel receivedY = (AnalogDataChannel) manager.getDataChannel("Longitudinal Acceleration");
		 AnalogDataChannel receivedZ = (AnalogDataChannel) manager.getDataChannel("Vertical Acceleration");
		 AnalogDataChannel receivedYaw = (AnalogDataChannel) manager.getDataChannel("Yaw Rate");		 
		 DataChannelGroup receivedInertialGroup = manager.getDataChannelGroup("Inertial Measurements");
		 
		 br = new BufferedReader(new FileReader(originalFileName));
		 rowNumber = 0;
		 line = br.readLine(); // get past the header line, no processing.
		 
		 int samplesReceived = 0;
		 int samplesSkipped = 0;
		 int errors = 0;
		 
		 while ((line = br.readLine()) != null) {
			 boolean validSample = receivedInertialGroup.isValid(rowNumber);
			 if (validSample) {
				 // Count as received
				 samplesReceived++;
				 
				 String[] columns = line.split(",");
				 
				 Float inertialValueX = Float.valueOf(columns[0]);
				 Float telemetryValueX = receivedX.getValue(rowNumber);
				 if (!inertialValueX.equals(telemetryValueX)) {
					 errors++;
					 System.out.println("Error, sample " + rowNumber + ". X Value expected " + inertialValueX + ", Received " + telemetryValueX);
				 }
				 				 
				 Float inertialValueY = Float.valueOf(columns[1]);
				 Float telemetryValueY = receivedY.getValue(rowNumber);
				 if (!inertialValueY.equals(telemetryValueY)) {
					 errors++;
					 System.out.println("Error, sample " + rowNumber + ". Y Value expected " + inertialValueY + ", Received " + telemetryValueY);
				 }
				 
				 Float inertialValueZ = Float.valueOf(columns[2]);
				 Float telemetryValueZ = receivedZ.getValue(rowNumber);
				 if (!inertialValueZ.equals(telemetryValueZ)) {
					 errors++;
					 System.out.println("Error, sample " + rowNumber + ". Z Value expected " + inertialValueZ + ", Received " + telemetryValueZ);
				 }
				 
				 Float inertialValueYaw = Float.valueOf(columns[3]);
				 Float telemetryValueYaw = receivedYaw.getValue(rowNumber);
				 if (!inertialValueYaw.equals(telemetryValueYaw)) {
					 errors++;
					 System.out.println("Error, sample " + rowNumber + ". Yaw Value expected " + inertialValueYaw + ", Received " + telemetryValueYaw);
				 }
				 
			 } else {
				 // Count as skipped
				 samplesSkipped++;
			 }
			 rowNumber++;
		 }
		 br.close();
		 
		 System.out.println("Received " + samplesReceived + " samples.");
		 System.out.println("Skipped " + samplesSkipped + " samples.");
		 System.out.println("Errors: " + errors);
	}

}
