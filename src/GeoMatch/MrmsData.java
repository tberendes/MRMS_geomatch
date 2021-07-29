package GeoMatch;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.LatLngTool.Bearing;
import com.javadocmd.simplelatlng.util.LengthUnit;

import Util.TempFile;
import Util.colormap;

public class MrmsData {
	static int INT_TYPE=0;
	static int FLOAT_TYPE=1;
	int ncols,nrows;
	float startLat, startLon, cellSize;
	boolean isCenterLL=true;
	double noDataValue=-999.0f;
	int dataType=FLOAT_TYPE;
	ByteBuffer binarySiteValues = null;
	BoundingBox imageBounds = null;
	
	public BoundingBox getImageBounds() {
		return imageBounds;
	}
	public ByteBuffer getBinarySiteValues() {
		return binarySiteValues;
	}
	ByteBuffer binaryFpMap = null;
	public ByteBuffer getBinaryFpMap() {
		return binaryFpMap;
	}

	float [][] floatData;
	int [][] intData;
	// startLat and startLon is inconsistent in the data, assume same fixed domain and hardcode
	public MrmsData(String fn, int type, float sLat, float sLon, float cSize, String tmpDir) throws IOException {
		String filename=fn;
		TempFile temp=null;
		try {
			if (filename.endsWith(".gz")) {
				temp = new TempFile(fn,tmpDir);
				filename = temp.getTempFilename();
//				System.out.println("temporary file: "+ filename);
				temp.unzip();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dataType=type;
		BufferedReader br = null;
		String currentLine = null;
		try
		{
			br = new BufferedReader(new FileReader(filename));
			// read header lines, split on whitespace
			currentLine = br.readLine();
			ncols = Integer.parseInt(currentLine.trim().split("\\s+")[1]);
			currentLine = br.readLine();
			nrows = Integer.parseInt(currentLine.trim().split("\\s+")[1]);
			// starting longitude
			currentLine = br.readLine();
			// startLat and startLon is inconsistent in the data, assume same fixed domain and hardcode
			startLon = sLon;
//			String [] tempStr = currentLine.trim().split("\\s+");
//			startLon = Double.parseDouble(tempStr[1]);

//			if (tempStr[0].equals("xllcorner")) {
//				isCenterLL=false;
//			} else if (tempStr[0].equals("xllcenter")) {				
//				isCenterLL=true;
//			} else {
//				System.out.println("Error, unknown lat/lon type " + tempStr[0]);
//				throw new IOException("Error, unknown lat/lon type ");
//			}
			// starting latitude
			currentLine = br.readLine();
//			startLat = Double.parseDouble(currentLine.trim().split("\\s+")[1]);	
			startLat = sLat;
			// cell size in degrees
			currentLine = br.readLine();
//			cellSize = Double.parseDouble(currentLine.trim().split("\\s+")[1]);		
			cellSize = cSize;		
			// missing data value
			currentLine = br.readLine();
			noDataValue = Float.parseFloat(currentLine.trim().split("\\s+")[1]);
			// Force precision of data to 3 decimal places for the lat/lon values, two for cellSize
			// round all lat/lon values to nearest thousandth 
//			startLon = (float)((int)(1000*(startLon+((startLon<0)?(-0.0005):0.0005))))/1000.0;
//			startLat = (float)((int)(1000*(startLat+((startLat<0)?(-0.0005):0.0005))))/1000.0;
//			cellSize = (float)((int)(100*(cellSize+0.005)))/100.0;
			
//			if (!isCenterLL) { // shift lat/lon interpretation to center of cell
//				startLon+=cellSize/2.0;
//				startLat+=cellSize/2.0;
//			}
//			
			// allocate array for values
			if (dataType==FLOAT_TYPE) {
				floatData = new float[nrows][ncols];
			} else if (dataType==INT_TYPE) {
				intData = new int[nrows][ncols];
			} else {
				System.out.println("Error, unknown data type ");
				throw new IOException("Error, unknown data type ");
			}
			//System.out.println("nrows "+nrows);
			//System.out.println("ncols "+ncols);
			//System.out.println("startLat "+startLat);
			//System.out.println("startLon "+startLon);
			//System.out.println("cellsize "+cellSize);
//from file:
//			ncols 7000
//			nrows 3500
//			xllcenter -129.995000
//			yllcenter 20.005000
//			cellsize 0.010000
//			NODATA_value -999
//			-999.00 -999.00 -999.00 -999.00 -999.00 -999.00 -999.00 -999.00 
// NOTE that the data is in descending latitude (i.e. UL to LL on map) order in the MRMS ASCII files
//   i.e. image display line order.  We switch to ascending latitude in order to more easily compute
//   lat and lon using startLat and startLon
			int lineCnt=0;
			while ((currentLine = br.readLine()) != null)
			{
				// parse line of text values into strings
				String [] strValues = currentLine.trim().split("\\s+");
				for (int ind1=0;ind1<strValues.length;ind1++){
					// assume first line in MRMS file is "top" latitude, swap rows of matrix 
					// to make latitude increase in matrix, this simplifies computation of offsets
					if (dataType==FLOAT_TYPE) {
						floatData[nrows-1-lineCnt][ind1]=Float.parseFloat(strValues[ind1]);
//						floatData[lineCnt][ind1]=Float.parseFloat(strValues[ind1]);
					}
					else {
						intData[nrows-1-lineCnt][ind1]=(int)Float.parseFloat(strValues[ind1]);						
//						intData[lineCnt][ind1]=(int)Float.parseFloat(strValues[ind1]);						
					}
				}
				lineCnt++;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			br.close();
			if (fn.endsWith(".gz")) {
				temp.deleteTemp();
			}
		}
	}
	public int getNcols() {
		return ncols;
	}
	public int getNrows() {
		return nrows;
	}
	public float[][] getFloatData() {
		return floatData;
	}
	public int[][] getIntData() {
		return intData;
	}
	private static BufferedImage createRGBImage(byte[] bytes, int width, int height) {
	    DataBufferByte buffer = new DataBufferByte(bytes, bytes.length);
	    ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
	    return new BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, new int[]{0, 1, 2}, null), false, null);
	}
	private static BufferedImage createGreyscaleImage(byte[] bytes, int width, int height) {
	    DataBufferByte buffer = new DataBufferByte(bytes, bytes.length);
	    ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[]{8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
	    return new BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width, 1,new int[]{0}, null), false, null);
	}
	public BufferedImage floatDataToImage(float min, float max, float centerlat, float centerlon, float siteRadiusKm, boolean colorFlag,boolean binaryDataFlag)
	/*
	 * creates grey scale image 0-255 between specified max/min values
	 */
	{
		
//		BufferedImage image = new BufferedImage(ncols, nrows, BufferedImage.TYPE_BYTE_GRAY);
		
		BoundingBox siteBounds = new BoundingBox(centerlat, centerlon, siteRadiusKm);	
		int startPix = siteBounds.getStartPix();
		int startLine= siteBounds.getStartLine();
		int endPix = siteBounds.getEndPix();
		int endLine= siteBounds.getEndLine();
		
		imageBounds = siteBounds;
		int numLines = endLine - startLine + 1;
		int numPix = endPix - startPix + 1;

		byte [][] allLines;
		if (colorFlag) {
			allLines = new byte[numLines][numPix*3];
		}
		else {
			allLines = new byte[numLines][numPix];
		}
		
		
		if (binaryDataFlag) {
			binarySiteValues=ByteBuffer.allocate(numLines*numPix*Float.BYTES+5*Float.BYTES);
			binarySiteValues.putFloat((float)numPix);		
			binarySiteValues.putFloat((float)numLines);		
			binarySiteValues.putFloat((float)getLat(startLine));
			binarySiteValues.putFloat((float)getLon(startPix));
			binarySiteValues.putFloat(cellSize);
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for (int lineInd=0,ind1=startLine;ind1<=endLine;ind1++,lineInd++){
			for (int pixInd=0,ind2=startPix;ind2<=endPix;ind2++,pixInd++){
				float value = floatData[ind1][ind2];
				if (binaryDataFlag) {
					binarySiteValues.putFloat(value);
				}
				byte byteValue;
				if (value<min) byteValue = 0;
				else if (value>max) byteValue = (byte)255;
				else byteValue=(byte) (255.0 * ( value - min) / (max - min));
				if (colorFlag) {
					int index=0xFF&byteValue;
					allLines[lineInd][pixInd*3] = colormap.radarScale[index][0];
					allLines[lineInd][pixInd*3+1] = colormap.radarScale[index][1];
					allLines[lineInd][pixInd*3+2] = colormap.radarScale[index][2];
				}
				else {
					allLines[lineInd][pixInd] = byteValue;
				}
			}
//			// draw one line at a time into bufferedImage
//			if (colorFlag)
//				bos.write(oneLine, 0, numPix*3);
//			else
//				bos.write(oneLine, 0, numPix);
		}
		// write image, flip line order for top to bottom image rendering
		for (int lineInd=0,ind1=startLine;ind1<=endLine;ind1++,lineInd++){
			
			// draw one line at a time into bufferedImage
			if (colorFlag)
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix*3);
			else
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix);;
		}
		byte[] pixels = bos.toByteArray();
		
		BufferedImage image;
		if (colorFlag)
			image = createRGBImage(pixels, numPix, numLines);
		else 
			image = createGreyscaleImage(pixels, numPix, numLines);
				
		return image;
	}
	public BufferedImage matchGPMToImage(float min, float max, float centerlat, float centerlon, float siteRadiusKm,ArrayList<LatLng> gpmLatLon, 
			float fpRadiusKm,ArrayList<Float> sfcPrecipRate, boolean colorFlag,boolean binaryDataFlag,boolean fpFlag)
	/*
	 * creates grey scale image 0-255 between specified max/min values
	 */
	{
		
//		BufferedImage image = new BufferedImage(ncols, nrows, BufferedImage.TYPE_BYTE_GRAY);

		
		BoundingBox siteBounds = new BoundingBox(centerlat, centerlon, siteRadiusKm);	
		int startPix = siteBounds.getStartPix();
		int startLine= siteBounds.getStartLine();
		int endPix = siteBounds.getEndPix();
		int endLine= siteBounds.getEndLine();
		int numLines = endLine - startLine + 1;
		int numPix = endPix - startPix + 1;
		
		imageBounds = siteBounds;
		
		float [][] binaryLines=null;
		float [][] binaryFpLines=null;
		
		if (binaryDataFlag) {
			binarySiteValues=ByteBuffer.allocate(numLines*numPix*Float.BYTES+5*Float.BYTES);
			binarySiteValues.putFloat((float)numPix);		
			binarySiteValues.putFloat((float)numLines);		
			binarySiteValues.putFloat((float)getLat(startLine));
			binarySiteValues.putFloat((float)getLon(startPix));
			binarySiteValues.putFloat(cellSize);
			binaryLines = new float[numLines][numPix];
		}
			
		if (fpFlag) {
			binaryFpMap=ByteBuffer.allocate(numLines*numPix*Integer.BYTES+5*Float.BYTES);
			binaryFpMap.putFloat((float)numPix);		
			binaryFpMap.putFloat((float)numLines);		
			binaryFpMap.putFloat((float)getLat(startLine));
			binaryFpMap.putFloat((float)getLon(startPix));
			binaryFpMap.putFloat(cellSize);
			binaryFpLines = new float[numLines][numPix];
		}

		byte [][] allLines;
		if (colorFlag) {
			allLines = new byte[numLines][numPix*3];
		}
		else {
			allLines = new byte[numLines][numPix];
		}
		// create blank image
		for (int ind1=0;ind1<numLines;ind1++){
			for (int ind2=0;ind2<numPix;ind2++){
				if (binaryDataFlag) {
					binaryLines[ind1][ind2] = -9999.0f;
				}
				if (fpFlag) {
					binaryFpLines[ind1][ind2] = -9999.0f;					
				}
				if (colorFlag) {
					allLines[ind1][ind2*3] = 0;
					allLines[ind1][ind2*3+1] = 0;
					allLines[ind1][ind2*3+2] = 0;
				}
				else
					allLines[ind1][ind2] = 0;
			}
		}
		// loop through GPM footprints and render footprint circles in allLines
		int footprintIndex=0;
		for (LatLng centerLoc:gpmLatLon) {
			float lat=(float)centerLoc.getLatitude();
			float lon=(float)centerLoc.getLongitude();
			BoundingBox fpBounds = new BoundingBox(lat, lon, fpRadiusKm);	
			int fpstartPix = fpBounds.getStartPix();
			int fpstartLine= fpBounds.getStartLine();
			int fpendPix = fpBounds.getEndPix();
			int fpendLine= fpBounds.getEndLine();
			float value = sfcPrecipRate.get(footprintIndex);
			byte byteValue;
			if (value<min) byteValue = 0;
			else if (value>max) byteValue = (byte)255;
			else byteValue=(byte) (255.0 * ( value - min) / (max - min));
			
			
			for (int ind1=fpstartLine; ind1<=fpendLine;ind1++) {
				if (ind1<startLine || ind1>endLine) continue;
				for (int ind2=fpstartPix;ind2<=fpendPix;ind2++) {
					if (ind2<startPix || ind2>endPix) continue;
					// check against radius to eliminate corners of boxed lat/long region
					// compute lat/lon of each "pixel"
					double fplat = getLat(ind1);
					double fplon = getLon(ind2);
					if (LatLngTool.distance(centerLoc, new LatLng(fplat,fplon), LengthUnit.KILOMETER) < fpRadiusKm ) {
						if (colorFlag) {
							int index=0xFF&byteValue;
							allLines[ind1 - startLine][(ind2 - startPix)*3] = colormap.radarScale[index][0];
							allLines[ind1 - startLine][(ind2 - startPix)*3+1] = colormap.radarScale[index][1];
							allLines[ind1 - startLine][(ind2 - startPix)*3+2] = colormap.radarScale[index][2];
						}
						else {
							allLines[ind1 - startLine][ind2 - startPix] = byteValue;
						}
						if (binaryDataFlag) {
							binaryLines[ind1 - startLine][ind2 - startPix] = value;
						}
						if (fpFlag) {
							binaryFpLines[ind1 - startLine][ind2 - startPix] = footprintIndex;
						}
					}
//					else {
//						allLines[ind1 - startLine][ind2 - startPix] = (byte)255;
//						
//					}
				}
			}
			footprintIndex++;
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// write image, flip line order for top to bottom image rendering
		for (int lineInd=0,ind1=startLine;ind1<=endLine;ind1++,lineInd++){
			// write out binary data to bytebuffer
			if (binaryLines!=null) {
				for (int pixInd=0,ind2=startPix;ind2<=endPix;ind2++,pixInd++){
					binarySiteValues.putFloat(binaryLines[lineInd][pixInd]);					
				}
			}
			if (binaryFpLines!=null) {
				for (int pixInd=0,ind2=startPix;ind2<=endPix;ind2++,pixInd++){
					binaryFpMap.putFloat(binaryFpLines[lineInd][pixInd]);					
				}
			}
			// draw one line at a time into bufferedImage
			if (colorFlag)
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix*3);
			else
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix);;
		}
		
		byte[] pixels = bos.toByteArray();

		BufferedImage image;
		if (colorFlag)
			image = createRGBImage(pixels, numPix, numLines);
		else 
			image = createGreyscaleImage(pixels, numPix, numLines);

		return image;
	}
	public float getFloatValue(int row, int col)
	{
		return floatData[row][col];
	}
	public int getIntValue(int row, int col)
	{
		return intData[row][col];
	}
	public float getFloatValue(double lat, double lon)
	{
		// return all values assuming lat/lon is relative to center of cell
		int col = (int)((lon - startLon)/cellSize);
		int row = (int)((lat - startLat)/cellSize);
		return floatData[row][col];
	}
	public int getIntValue(double lat, double lon)
	{
		// return all values assuming lat/lon is relative to center of cell
		int col = (int)((lon - startLon)/cellSize);
		int row = (int)((lat - startLat)/cellSize);
		return intData[row][col];
	}
	public Point getGridCoords(double lat, double lon)
	{
		int col = (int)((lon - startLon)/cellSize);
		int row = (int)((lat - startLat)/cellSize);
		// error check
		if (row<0) row=0;
		if (col<0) col=0;
		if (row>=nrows) row= nrows-1;
		if (col>=ncols) col=ncols-1;
		return new Point(col,row);
	}
	public double getLat(int row) 
	{
		return (double)row*cellSize + startLat;
	}
	public double getLon(int col) 
	{
		return (double)col*cellSize + startLon;
	}
	public ArrayList<Float> getSiteRadiusValuesFloat(LatLng footprintLoc, double radius) {
		
		ArrayList<Float> values = new ArrayList<>();
		// find closest MRMS grid point over center of footprint
		Point centerMRMS=getGridCoords(footprintLoc.getLatitude(),footprintLoc.getLongitude());
		double centerlat = getLat((int)centerMRMS.getY());
		double centerlon = getLon((int)centerMRMS.getX());
		
		BoundingBox siteBounds = new BoundingBox((float)centerlat, (float)centerlon, (float)radius);	
		int startPix = siteBounds.getStartPix();
		int startLine= siteBounds.getStartLine();
		int endPix = siteBounds.getEndPix();
		int endLine= siteBounds.getEndLine();
		LatLng centerLoc=new LatLng(centerlat, centerlon);
//		
//		// compute bounding box for a 2.6km radius (5.2km nadir resolution DPR footprint)
////		LatLng llCorner = LatLngTool.travel(centerLoc, Bearing.SOUTH_WEST, radius,LengthUnit.KILOMETER);
////		LatLng urCorner = LatLngTool.travel(centerLoc, Bearing.NORTH_EAST, radius,LengthUnit.KILOMETER);
////		Point startPt = getGridCoords(llCorner.getLatitude(), llCorner.getLongitude());
////		Point endPt = getGridCoords(urCorner.getLatitude(), urCorner.getLongitude());
//		LatLng up = LatLngTool.travel(centerLoc, Bearing.NORTH, radius,LengthUnit.KILOMETER);
//		LatLng down = LatLngTool.travel(centerLoc, Bearing.SOUTH, radius,LengthUnit.KILOMETER);
//		LatLng right = LatLngTool.travel(centerLoc, Bearing.EAST, radius,LengthUnit.KILOMETER);
//		LatLng left = LatLngTool.travel(centerLoc, Bearing.WEST, radius,LengthUnit.KILOMETER);
//		Point startPt = getGridCoords(down.getLatitude(), left.getLongitude());
//		Point endPt = getGridCoords(up.getLatitude(), right.getLongitude());
//		int startPix = (int)startPt.getX();
//		int startLine= (int)startPt.getY();
//		int endPix = (int)endPt.getX();
//		int endLine= (int)endPt.getY();
		
		
		// loop through array from start to end
		for (int ind1=0; ind1<=endLine-startLine;ind1++) {
			for (int ind2=0;ind2<=endPix-startPix;ind2++) {
				// check against radius to eliminate corners of boxed lat/long region
				// compute lat/lon of each "pixel"
				double lat = getLat(startLine + ind1);
				double lon = getLon(startPix + ind2);
				if (LatLngTool.distance(centerLoc, new LatLng(lat,lon), LengthUnit.KILOMETER)<= radius) {
					values.add(floatData[startLine + ind1][startPix + ind2]);
//					if (removeNegatives) {
//						if ( floatData[startLine + ind1][startPix + ind2]>=0) {
//							values.add(floatData[startLine + ind1][startPix + ind2]);	
//						}
//					}
//					else {
//						values.add(floatData[startLine + ind1][startPix + ind2]);
//					}
				}
			}
		}
		return values;
	}
	public ArrayList<Integer> getSiteRadiusValuesInt(LatLng footprintLoc, double radius) {
		
		ArrayList<Integer> values = new ArrayList<>();
		// find closest MRMS grid point over center of footprint
		Point centerMRMS=getGridCoords(footprintLoc.getLatitude(),footprintLoc.getLongitude());
		double centerlat = getLat((int)centerMRMS.getY());
		double centerlon = getLon((int)centerMRMS.getX());
		
		BoundingBox siteBounds = new BoundingBox((float)centerlat, (float)centerlon, (float)radius);	
		int startPix = siteBounds.getStartPix();
		int startLine= siteBounds.getStartLine();
		int endPix = siteBounds.getEndPix();
		int endLine= siteBounds.getEndLine();
		LatLng centerLoc=new LatLng(centerlat, centerlon);

//		// compute bounding box for a 2.6km radius (5.2km nadir resolution DPR footprint)
////		LatLng llCorner = LatLngTool.travel(centerLoc, Bearing.SOUTH_WEST, radius,LengthUnit.KILOMETER);
////		LatLng urCorner = LatLngTool.travel(centerLoc, Bearing.NORTH_EAST, radius,LengthUnit.KILOMETER);
////		Point startPt = getGridCoords(llCorner.getLatitude(), llCorner.getLongitude());
////		Point endPt = getGridCoords(urCorner.getLatitude(), urCorner.getLongitude());
//		LatLng up = LatLngTool.travel(centerLoc, Bearing.NORTH, radius,LengthUnit.KILOMETER);
//		LatLng down = LatLngTool.travel(centerLoc, Bearing.SOUTH, radius,LengthUnit.KILOMETER);
//		LatLng right = LatLngTool.travel(centerLoc, Bearing.EAST, radius,LengthUnit.KILOMETER);
//		LatLng left = LatLngTool.travel(centerLoc, Bearing.WEST, radius,LengthUnit.KILOMETER);
//		Point startPt = getGridCoords(down.getLatitude(), left.getLongitude());
//		Point endPt = getGridCoords(up.getLatitude(), right.getLongitude());
//		int startPix = (int)startPt.getX();
//		int startLine= (int)startPt.getY();
//		int endPix = (int)endPt.getX();
//		int endLine= (int)endPt.getY();
		
		// loop through array from start to end
//		int pixcnt=(endPix - startPix + 1);
//		int linecnt=(endLine-startLine + 1);
//		System.out.println("stpix " + startPix + " endpix " + endPix);
//		System.out.println("stlin " + startLine + " endLin " + endLine);
		
//		int tooFar=0;
		for (int ind1=0; ind1<=endLine-startLine;ind1++) {
			for (int ind2=0;ind2<=endPix-startPix;ind2++) {
				// check against radius to eliminate corners of boxed lat/long region
				// compute lat/lon of each "pixel"
				double lat = getLat(startLine + ind1);
				double lon = getLon(startPix + ind2);
				if (LatLngTool.distance(centerLoc, new LatLng(lat,lon), LengthUnit.KILOMETER)<= radius) {
					values.add(intData[startLine + ind1][startPix + ind2]);
//					if (removeNegatives) {
//						if ( intData[startLine + ind1][startPix + ind2]>=0) {
//							values.add(intData[startLine + ind1][startPix + ind2]);	
//						}
//					}
//					else {
//						values.add(intData[startLine + ind1][startPix + ind2]);
//					}
				}
//				else {
//					System.out.println("distance " + LatLngTool.distance(centerLoc, new LatLng(lat,lon), LengthUnit.KILOMETER));
//					tooFar++;
//				}
			}
		}
//		System.out.println( pixcnt+ "x" + linecnt + ": " + values.size() + " in " + tooFar +" out");
		return values;
	}
	public static BufferedImage bytebufferToImage(ByteBuffer buffer, float min, float max, float centerlat, float centerlon, float siteRadiusKm, boolean colorFlag)
	/*
	 * creates grey scale image 0-255 between specified max/min values
	 */
	{
		
//		BufferedImage image = new BufferedImage(ncols, nrows, BufferedImage.TYPE_BYTE_GRAY);
		
		// read header from bytebuffer
		// rewind buffer
		buffer.rewind();
		int numPix = (int)(buffer.getFloat());		
		int numLines = (int)(buffer.getFloat());		
		int startPix = (int)(buffer.getFloat());		
		int startLine = (int)(buffer.getFloat());	
		float cellSize = buffer.getFloat();
		
		byte [][] allLines;
		if (colorFlag) {
			allLines = new byte[numLines][numPix*3];
		}
		else {
			allLines = new byte[numLines][numPix];
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for (int ind1=0;ind1<numLines;ind1++){
			for (int ind2=0;ind2<numPix;ind2++){
				float value = buffer.getFloat();
				byte byteValue;
				if (value<min) byteValue = 0;
				else if (value>max) byteValue = (byte)255;
				else byteValue=(byte) (255.0 * ( value - min) / (max - min));
				if (colorFlag) {
					int index=0xFF&byteValue;
					allLines[ind1][ind2*3] = colormap.radarScale[index][0];
					allLines[ind1][ind2*3+1] = colormap.radarScale[index][1];
					allLines[ind1][ind2*3+2] = colormap.radarScale[index][2];
				}
				else {
					allLines[ind1][ind2] = byteValue;
				}
			}
			
		}
		// write image, flip line order for top to bottom image rendering
		for (int ind1=0;ind1<numLines;ind1++){
			
			// draw one line at a time into bufferedImage
			if (colorFlag)
				bos.write(allLines[numLines - 1 - ind1], 0, numPix*3);
			else
				bos.write(allLines[numLines - 1 - ind1], 0, numPix);;
		}
		byte[] pixels = bos.toByteArray();
		
		BufferedImage image;
		if (colorFlag)
			image = createRGBImage(pixels, numPix, numLines);
		else 
			image = createGreyscaleImage(pixels, numPix, numLines);
				
		return image;
	}
		
	public class BoundingBox
	{
		int startPix;
		int startLine;
		int endPix;
		int endLine;
		LatLng north,south,east,west;
		public LatLng getNorth() {
			return north;
		}

		public LatLng getSouth() {
			return south;
		}

		public LatLng getEast() {
			return east;
		}

		public LatLng getWest() {
			return west;
		}
		
		public int getStartPix() {
			return startPix;
		}

		public int getStartLine() {
			return startLine;
		}

		public int getEndPix() {
			return endPix;
		}

		public int getEndLine() {
			return endLine;
		}		
		public BoundingBox(float centerLat, float centerLon, float radiusKm) 
		{
			LatLng centerLoc=new LatLng(centerLat, centerLon);
			// compute bounding box for a radius around a lat/lon location
			north = LatLngTool.travel(centerLoc, Bearing.NORTH, radiusKm,LengthUnit.KILOMETER);
			south = LatLngTool.travel(centerLoc, Bearing.SOUTH, radiusKm,LengthUnit.KILOMETER);
			east = LatLngTool.travel(centerLoc, Bearing.EAST, radiusKm,LengthUnit.KILOMETER);
			west = LatLngTool.travel(centerLoc, Bearing.WEST, radiusKm,LengthUnit.KILOMETER);
			Point startPt = getGridCoords(south.getLatitude(), west.getLongitude());
			Point endPt = getGridCoords(north.getLatitude(), east.getLongitude());
			startPix = (int)startPt.getX();
			startLine= (int)startPt.getY();
			endPix = (int)endPt.getX();
			endLine= (int)endPt.getY();
		}
	}
}
