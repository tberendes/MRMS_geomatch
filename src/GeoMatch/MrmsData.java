package GeoMatch;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.LatLngTool.Bearing;
import com.javadocmd.simplelatlng.util.LengthUnit;

import Util.TempFile;

public class MrmsData {
	static int INT_TYPE=0;
	static int FLOAT_TYPE=1;
	int ncols,nrows;
	float startLat, startLon, cellSize;
	boolean isCenterLL=true;
	double noDataValue=-999.0f;
	int dataType=FLOAT_TYPE;
	
	float [][] floatData;
	int [][] intData;
	// startLat and startLon is inconsistent in the data, assume same fixed domain and hardcode
	public MrmsData(String fn, int type, float sLat, float sLon, float cSize) throws IOException {
		String filename=fn;
		TempFile temp=null;
		try {
			if (filename.endsWith(".gz")) {
				temp = new TempFile(fn);
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
			System.out.println("nrows "+nrows);
			System.out.println("ncols "+ncols);
			System.out.println("startLat "+startLat);
			System.out.println("startLon "+startLon);
			System.out.println("cellsize "+cellSize);
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
		LatLng centerLoc=new LatLng(centerlat, centerlon);
		
		// compute bounding box for a 2.6km radius (5.2km nadir resolution DPR footprint)
//		LatLng llCorner = LatLngTool.travel(centerLoc, Bearing.SOUTH_WEST, radius,LengthUnit.KILOMETER);
//		LatLng urCorner = LatLngTool.travel(centerLoc, Bearing.NORTH_EAST, radius,LengthUnit.KILOMETER);
//		Point startPt = getGridCoords(llCorner.getLatitude(), llCorner.getLongitude());
//		Point endPt = getGridCoords(urCorner.getLatitude(), urCorner.getLongitude());
		LatLng up = LatLngTool.travel(centerLoc, Bearing.NORTH, radius,LengthUnit.KILOMETER);
		LatLng down = LatLngTool.travel(centerLoc, Bearing.SOUTH, radius,LengthUnit.KILOMETER);
		LatLng right = LatLngTool.travel(centerLoc, Bearing.EAST, radius,LengthUnit.KILOMETER);
		LatLng left = LatLngTool.travel(centerLoc, Bearing.WEST, radius,LengthUnit.KILOMETER);
		Point startPt = getGridCoords(down.getLatitude(), left.getLongitude());
		Point endPt = getGridCoords(up.getLatitude(), right.getLongitude());
		int startPix = (int)startPt.getX();
		int startLine= (int)startPt.getY();
		int endPix = (int)endPt.getX();
		int endLine= (int)endPt.getY();
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
		LatLng centerLoc=new LatLng(centerlat, centerlon);

		// compute bounding box for a 2.6km radius (5.2km nadir resolution DPR footprint)
//		LatLng llCorner = LatLngTool.travel(centerLoc, Bearing.SOUTH_WEST, radius,LengthUnit.KILOMETER);
//		LatLng urCorner = LatLngTool.travel(centerLoc, Bearing.NORTH_EAST, radius,LengthUnit.KILOMETER);
//		Point startPt = getGridCoords(llCorner.getLatitude(), llCorner.getLongitude());
//		Point endPt = getGridCoords(urCorner.getLatitude(), urCorner.getLongitude());
		LatLng up = LatLngTool.travel(centerLoc, Bearing.NORTH, radius,LengthUnit.KILOMETER);
		LatLng down = LatLngTool.travel(centerLoc, Bearing.SOUTH, radius,LengthUnit.KILOMETER);
		LatLng right = LatLngTool.travel(centerLoc, Bearing.EAST, radius,LengthUnit.KILOMETER);
		LatLng left = LatLngTool.travel(centerLoc, Bearing.WEST, radius,LengthUnit.KILOMETER);
		Point startPt = getGridCoords(down.getLatitude(), left.getLongitude());
		Point endPt = getGridCoords(up.getLatitude(), right.getLongitude());
		int startPix = (int)startPt.getX();
		int startLine= (int)startPt.getY();
		int endPix = (int)endPt.getX();
		int endLine= (int)endPt.getY();
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
		
}
