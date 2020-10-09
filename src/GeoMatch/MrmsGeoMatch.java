package GeoMatch;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.io.Files;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import GeoMatch.MrmsData.BoundingBox;
import Util.TempFile;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayString;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

public class MrmsGeoMatch {

	static boolean TEST_MODE=false;
	static int [] HISTOGRAM_INDICES = {-1,0,1,2,3,4,6,7,91,96,10};
	static String [] HISTOGRAM_CODES = {"missing","no precip", "warm stratiform rain"
			,"warm stratiform rain at ground, radar data in or above melting layer"
			,"snow", "snow at ground, radar data 1.5km or higher above ground"
			,"convective", "hail", "tropical/stratiform rain mix"
			,"tropical/convecive rain mix", "cool stratiform rain"};
	
	Map<Integer, String> maskLabels = new HashMap<>();
	double DPR_FOOTPRINT = 5.2;
	String vnInputDir;
	String vnOutputDir;
	String mrmsPath;
	public MrmsGeoMatch(String InputDir, String OutputDir, String mrmsPath)
	{
		this.mrmsPath = mrmsPath;
		this.vnInputDir = InputDir;
		this.vnOutputDir = OutputDir;
    	// initialize histogram labels
    	for (int ind1=0;ind1<HISTOGRAM_INDICES.length;ind1++)
    		maskLabels.put(HISTOGRAM_INDICES[ind1], HISTOGRAM_CODES[ind1]);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String vnInputDir;
		String vnOutputDir;
		String mrmsPath;
		boolean overwriteFlag = false;
		
		if (!TEST_MODE) {
			if (args.length<3) {
				System.out.println("Usage:  java -jar MrmsGeoMatch input_VN_directory output_VN_directory MRMS_root_directory <overwrite_if_param_present>");
				System.out.println("  Matches MRMS data with GPM VN matchup files and creates new netCDF files");
				System.exit(-1);
			}
			vnInputDir = args[0];
			vnOutputDir = args[1];
			mrmsPath = args[2];
			if (args.length == 4) {
				overwriteFlag = true;
			}
		}
		else {
			// testing only, remove these three lines and uncomment above block
			vnInputDir = "C:\\Users\\tberendes\\gpmgv\\input_dir";
			vnOutputDir = "C:\\Users\\tberendes\\gpmgv\\output_dir";
			mrmsPath = "C:\\Users\\tberendes\\gpmgv\\MRMS\\level2";
		}

		if (vnInputDir.equals(vnOutputDir)) {
			System.out.println("Error: output directory must be different from input directory");
			System.exit(-1);
		}
		
		File outDir = new File (vnOutputDir);
		if (outDir.exists()&& outDir.isDirectory()) {
			System.out.println("Output directory " + vnOutputDir + " exists");
			
		}
		else if (outDir.isFile()){
			System.out.println("Error:  Output directory path " + vnOutputDir + " is a file not a directory");
			System.exit(-1);
		}
		else {
			System.out.println("Creating output directory " + vnOutputDir);		
			outDir.mkdirs();
		}
		if (!outDir.exists()) {
			System.out.println("Error: could not create output directory " + vnOutputDir);
			System.exit(-1);
		}
		
		MrmsGeoMatch mrmsGeo = new MrmsGeoMatch(vnInputDir,vnOutputDir,mrmsPath);
		
 
		if (!TEST_MODE) {
			mrmsGeo.processDirectory(overwriteFlag);
		}
		else {
		// testing only, remove these three lines and uncomment above line
		
	//		String vnInputFilename = "C:\\Users\\tberendes\\gpmgv\\GPM_matchups\\GRtoDPR.KHTX.160626.13215.ITE114.DPR.NS.1_21.15dbzGRDPR_newDm.nc.gz";
	//		String vnOutputFilename = "C:\\Users\\tberendes\\gpmgv\\GPM_matchups\\GRtoDPR.KHTX.160626.13215.ITE114.DPR.NS.1_21.15dbzGRDPR_newDm_mrms.nc";
			String vnInputFilename = "C:\\Users\\tberendes\\gpmgv\\MRMS\\GRtoDPR.KDOX.140907.2977.V05A.DPR.NS.1_21.nc.gz";
			String vnOutputFilename = "C:\\Users\\tberendes\\gpmgv\\MRMS\\GRtoDPR.KDOX.140907.2977.V05A.DPR.NS.1_21_mrms.nc.gz";
			mrmsGeo.processFile(vnInputFilename, vnOutputFilename);
		}
		
	}
	void processDirectory(boolean overwriteFlag)
	{	
		File dsFile = new File (vnInputDir);
    	File [] dirListing = dsFile.listFiles();
    	if (dirListing==null || dirListing.length==0) {  // no dates are processed
    		try {
				System.out.println("no files to process in " + dsFile.getCanonicalPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return;
    	}
		// loop over files in given directory
    	for (int ind1=0;ind1<dirListing.length;ind1++) {
    		// only process files, skip directories
    		if (dirListing[ind1].exists()&&dirListing[ind1].isFile()) {
        		String filename = dirListing[ind1].getName();
//        		System.out.println("filename "+ filename);
        		String [ ] arrStr = filename.split("\\.");
        		        		
        		// check for each file type
    			if (filename.endsWith(".nc.gz") || filename.endsWith(".nc")) {
    				String vnInputFilename = vnInputDir + File.separator + filename;
    				String vnOutputFilename = vnOutputDir + File.separator + filename;
    				
					// check to see if file exists, and only process if it doesn't
					File vnfile = new File (vnOutputFilename);
					if (vnfile.exists()) {
	    				if (overwriteFlag) {
    						System.out.println("file " + vnOutputFilename + " exists, overwriting...");	    					
	    					processFile(vnInputFilename, vnOutputFilename);
	    				}
	    				else {
    						System.out.println("file " + vnOutputFilename + " exists, skipping...");	    					
	    				}
					}
					else {
    					processFile(vnInputFilename, vnOutputFilename);
						
					}
    			}
    		}
    	}
	
	}
	
	void processFile(String vnInputFilename, String vnOutputFilename)
	{
		NetcdfFileWriter mFptr=null;
		String gpmTime=null;
		float siteLat=0.0f;
		float siteLon=0.0f;
		String filename;
		TempFile temp=null;
		Array dprLatitude=null, dprLongitude=null;
		Array PrecipRateSurface=null, SurfPrecipTotRate=null;
		Array PrecipRate=null;
		Dimension fpdim=null;
		Mrms mrms=null;
		try {
			temp = new TempFile(vnInputFilename);
			filename = temp.getTempFilename();
			if (vnInputFilename.endsWith(".gz")) {
//				System.out.println("temporary file: "+ filename);
				temp.unzip();
			}
			else {
				temp.copy();
			}
			
			mFptr = NetcdfFileWriter.openExisting(filename);
			Variable var = mFptr.findVariable("atimeNearestApproach");
			if (var==null) {
				System.err.println("cannot find variable atimeNearestApproach");
				throw new IOException();			
			}
			int[] shape = var.getShape(); // array dimensions
			gpmTime = var.readScalarString();
			System.out.println("gpmTime: " + gpmTime);
//			Array arr = var.read();
			//ByteBuffer buff = arr.getDataAsByteBuffer();

			fpdim = mFptr.getNetcdfFile().findDimension("fpdim");
			var = mFptr.findVariable("site_lat");
			if (var==null) {
				System.err.println("cannot find variable site_lat");
				throw new IOException();			
			}
			siteLat = var.readScalarFloat();
			System.out.println("site_lat: " + siteLat);
			var = mFptr.findVariable("site_lon");
			if (var==null) {
				System.err.println("cannot find variable site_lon");
				throw new IOException();			
			}
			siteLon = var.readScalarFloat();
			System.out.println("site_lat: " + siteLon);

			var = mFptr.findVariable("DPRlatitude");
			if (var==null) {
				System.err.println("cannot find variable DPRlatitude");
				throw new IOException();			
			}
			dprLatitude = var.read();
			long numFp = dprLatitude.getSize();
			var = mFptr.findVariable("DPRlongitude");
			if (var==null) {
				System.err.println("cannot find variable DPRlongitude");
				throw new IOException();			
			}
			dprLongitude = var.read();

			// DPR precip rate (elev,fpdim)
			var = mFptr.findVariable("PrecipRate");
			if (var==null) {
				System.err.println("cannot find variable PrecipRate");
				throw new IOException();			
			}
			PrecipRate = var.read();
			
			// DPR near sfc precip rate (fpdim)
			var = mFptr.findVariable("PrecipRateSurface");
			if (var==null) {
				System.err.println("cannot find variable PrecipRateSurface");
				throw new IOException();			
			}
			PrecipRateSurface = var.read();
			// DPRGMI near sfc, not present in DPR file (fpdim)
			var = mFptr.findVariable("SurfPrecipTotRate");
			if (var==null) {
				System.err.println("cannot find variable SurfPrecipTotRate");
				throw new IOException();			
			}
			SurfPrecipTotRate = var.read();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			mrms = new Mrms(gpmTime, mrmsPath, siteLat, siteLon);
	    	
	    	// enter define mode 
			mFptr.setRedefineMode(true);
			mFptr.flush();

	    	// add new dimension and variables to netcdf file
			Dimension mrmsMaskDim = mFptr.addDimension(null, "mrms_mask", 11);

//			Variable maskLabels = mFptr.addVariable(null, "MRMSMaskLabels", DataType.STRING, "mrms_mask");
//			ArrayString labels = new ArrayString.D1(mrmsMaskDim.getLength());
//			Index index=labels.getIndex();
//			int ind1=0;
//			for (String str:HISTOGRAM_CODES) {
//				labels.set(index.set(ind1++), str);
//			}
//			int[] origin = new int[2];
//			// write out variable
//			mFptr.write( maskLabels, origin, labels);
			
			String MRMSmaskStr="";
			for (int ind:HISTOGRAM_INDICES) {
				MRMSmaskStr = MRMSmaskStr + ind + "-" + maskLabels.get(ind) + "; ";
			}
			mFptr.addGroupAttribute(null, new Attribute("MRMS Mask categories", MRMSmaskStr));
			 
			// add new variables to the temporary file
			Variable precipMeanLowVar = mFptr.addVariable(null, "PrecipMeanLow", DataType.FLOAT, "fpdim");
			precipMeanLowVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanLowVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .5"));
			Variable precipMeanMedVar = mFptr.addVariable(null, "PrecipMeanMed", DataType.FLOAT, "fpdim");
			precipMeanMedVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanMedVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .75"));
			Variable precipMeanHighVar = mFptr.addVariable(null, "PrecipMeanHigh", DataType.FLOAT, "fpdim");
			precipMeanHighVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanHighVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .9"));
			Variable precipMeanVeryHighVar = mFptr.addVariable(null, "PrecipMeanVeryHigh", DataType.FLOAT, "fpdim");
			precipMeanVeryHighVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanVeryHighVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .95"));

			Variable guageRatioMeanLowVar = mFptr.addVariable(null, "GuageRatioMeanLow", DataType.FLOAT, "fpdim");
			guageRatioMeanLowVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanLowVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with RQI >= .5"));
			Variable guageRatioMeanMedVar = mFptr.addVariable(null, "GuageRatioMeanMed", DataType.FLOAT, "fpdim");
			guageRatioMeanMedVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanMedVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with RQI >= .75"));
			Variable guageRatioMeanHighVar = mFptr.addVariable(null, "GuageRatioMeanHigh", DataType.FLOAT, "fpdim");
			guageRatioMeanHighVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanHighVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with with RQI >= .9"));
			Variable guageRatioMeanVeryHighVar = mFptr.addVariable(null, "GuageRatioMeanVeryHigh", DataType.FLOAT, "fpdim");
			guageRatioMeanVeryHighVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanVeryHighVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with RQI >= .95"));

			// add new variables for RQI percentage of MRMS points to the temporary file
			Variable rqiPercentLowVar = mFptr.addVariable(null, "RqiPercentLow", DataType.FLOAT, "fpdim");
			rqiPercentLowVar.addAttribute(new Attribute("description", "Percent of MRMS grid points with RQI >= .5"));
			Variable rqiPercentMedVar = mFptr.addVariable(null, "RqiPercentMed", DataType.FLOAT, "fpdim");
			rqiPercentMedVar.addAttribute(new Attribute("description", "Percent of MRMS grid points with RQI >= .75"));
			Variable rqiPercentHighVar = mFptr.addVariable(null, "RqiPercentHigh", DataType.FLOAT, "fpdim");
			rqiPercentHighVar.addAttribute(new Attribute("description", "Percent of MRMS grid points with RQI >= .9"));
			Variable rqiPercentVeryHighVar = mFptr.addVariable(null, "RqiPercentVeryHigh", DataType.FLOAT, "fpdim");
			rqiPercentVeryHighVar.addAttribute(new Attribute("description", "Percent of MRMS grid points with RQI >= .95"));
			
			List<Dimension> dims = new ArrayList<Dimension>();
		    dims.add(fpdim);
		    dims.add(mrmsMaskDim);
		    
		    // add scalar varialble flag have_MRMS for presence of mrms data, use empty string for dimensions
		    Variable mrmsFlagVar = mFptr.addVariable(null, "have_MRMS", DataType.SHORT, "");
		    mrmsFlagVar.addAttribute(new Attribute("long_name", "Data exists for MRMS rain rates"));
		    mrmsFlagVar.addAttribute(new Attribute("_FillValue", (short)0));

		    Variable maskLowVar = mFptr.addVariable(null, "MaskLow", DataType.FLOAT, dims);
			maskLowVar.addAttribute(new Attribute("units", "none"));
			maskLowVar.addAttribute(new Attribute("description", "MRMS categories with RQI >= .5"));
			Variable maskMedVar = mFptr.addVariable(null, "MaskMed", DataType.FLOAT, dims);
			maskMedVar.addAttribute(new Attribute("units", "none"));
			maskMedVar.addAttribute(new Attribute("description", "MRMS categories with RQI >= .75"));
			Variable maskHighVar = mFptr.addVariable(null, "MaskHigh", DataType.FLOAT, dims);
			maskHighVar.addAttribute(new Attribute("units", "none"));
			maskHighVar.addAttribute(new Attribute("description", "MRMS categories RQI with RQI >= .9"));
			Variable maskVeryHighVar = mFptr.addVariable(null, "MaskVeryHigh", DataType.FLOAT, dims);
			maskVeryHighVar.addAttribute(new Attribute("units", "none"));
			maskVeryHighVar.addAttribute(new Attribute("description", "MRMS categories with RQI >= .95"));

		
	    	// leave define mode 
			mFptr.flush();
			mFptr.setRedefineMode(false);
			mFptr.flush();
		
			// allocate arrays for variables
			ArrayFloat precipMeanLowArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat precipMeanMedArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat precipMeanHighArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat precipMeanVeryHighArr = new ArrayFloat.D1(fpdim.getLength());
			
			ArrayFloat guageRatioMeanLowArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat guageRatioMeanMedArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat guageRatioMeanHighArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat guageRatioMeanVeryHighArr = new ArrayFloat.D1(fpdim.getLength());
			
			int [] shape = maskLowVar.getShape();
			ArrayInt maskLowArr = new ArrayInt.D2(shape[0], shape[1]);
			ArrayInt maskMedArr = new ArrayInt.D2(shape[0], shape[1]);
			ArrayInt maskHighArr = new ArrayInt.D2(shape[0], shape[1]);
			ArrayInt maskVeryHighArr = new ArrayInt.D2(shape[0], shape[1]);
			
			// allocate arrays for MRMS RQI percent 
			ArrayFloat rqiPercentLowArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat rqiPercentMedArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat rqiPercentHighArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat rqiPercentVeryHighArr = new ArrayFloat.D1(fpdim.getLength());

			// loop through DPR footprints
			for (int ind1=0;ind1<fpdim.getLength();ind1++) {
				LatLng latlon = new LatLng(dprLatitude.getDouble(ind1),dprLongitude.getDouble(ind1));
				// assume all MRMS data files are at same resolution and same area coverage
				ArrayList<Float> RQIArray = mrms.getRQIdata().getSiteRadiusValuesFloat(latlon, DPR_FOOTPRINT/2.0);
				ArrayList<Float> PRECIPArray = mrms.getPRECIPRATEdata().getSiteRadiusValuesFloat(latlon, DPR_FOOTPRINT/2.0);
//				System.out.println("precipArray " + PRECIPArray.size() + " " + PRECIPArray);
				ArrayList<Float> HCFArray = mrms.getHCFdata().getSiteRadiusValuesFloat(latlon, DPR_FOOTPRINT/2.0);
				
				// include negative values in MASK
				ArrayList<Integer> MASKArray = mrms.getMASKdata().getSiteRadiusValuesInt(latlon, DPR_FOOTPRINT/2.0);
			// define four RQI bins 0-.5, .51-.75, .76-.99, 1.0
				
				
				DescriptiveStatistics precipLow=filteredStats(PRECIPArray,RQIArray,0.5,100.0);
				DescriptiveStatistics precipMed=filteredStats(PRECIPArray,RQIArray,0.75,100.0);
				DescriptiveStatistics precipHigh=filteredStats(PRECIPArray,RQIArray,0.9,100.0);
				DescriptiveStatistics precipVeryHigh=filteredStats(PRECIPArray,RQIArray,0.95,100);
				
				// set up RQIPercentLow, med, high, veryhigh
				float rqiPercentLow=percentAbove(RQIArray,0.5,100.0);
				float rqiPercentMed=percentAbove(RQIArray,0.75,100.0);
				float rqiPercentHigh=percentAbove(RQIArray,0.90,100.0);
				float rqiPercentVeryHigh=percentAbove(RQIArray,0.95,100.0);
				
				rqiPercentLowArr.setFloat(rqiPercentLowArr.getIndex().set(ind1),rqiPercentLow);
				rqiPercentMedArr.setFloat(rqiPercentMedArr.getIndex().set(ind1),rqiPercentMed);
				rqiPercentHighArr.setFloat(rqiPercentHighArr.getIndex().set(ind1),rqiPercentHigh);
				rqiPercentVeryHighArr.setFloat(rqiPercentVeryHighArr.getIndex().set(ind1),rqiPercentVeryHigh);
				
				// compute mean, precip rate, HCF (gauge ratios)
				DescriptiveStatistics guageRatioLow=filteredStats(HCFArray,RQIArray,0.5,100.0);
				DescriptiveStatistics guageRatioMed=filteredStats(HCFArray,RQIArray,0.75,100.0);
				DescriptiveStatistics guageRatioHigh=filteredStats(HCFArray,RQIArray,0.9,100.0);
				DescriptiveStatistics guageRatioVeryHigh=filteredStats(HCFArray,RQIArray,0.95,100.0);
				
				// compute histogram of mask values
				Map<Integer, Integer> maskMapLow = maskHistogram(MASKArray,RQIArray,0.5,100.0);
				Map<Integer, Integer> maskMapMed=maskHistogram(MASKArray,RQIArray,0.75,100.0);
				Map<Integer, Integer> maskMapHigh=maskHistogram(MASKArray,RQIArray,0.9,100.0);
				Map<Integer, Integer> maskMapVeryHigh=maskHistogram(MASKArray,RQIArray,0.95,100.0);
				
				Set<Integer> keyset = maskMapLow.keySet();
				int ind2=0;

				for (Integer key:keyset){
					maskLowArr.setInt(maskLowArr.getIndex().set(ind1,ind2), maskMapLow.get(key));
					maskMedArr.setInt(maskMedArr.getIndex().set(ind1,ind2), maskMapMed.get(key));
					maskHighArr.setInt(maskHighArr.getIndex().set(ind1,ind2), maskMapHigh.get(key));
					maskVeryHighArr.setInt(maskVeryHighArr.getIndex().set(ind1,ind2), maskMapVeryHigh.get(key));
					ind2++;
				}
				
				precipMeanLowArr.setFloat(precipMeanLowArr.getIndex().set(ind1), precipLow==null?-999.0f:(float)precipLow.getMean());
				precipMeanMedArr.setFloat(precipMeanMedArr.getIndex().set(ind1), precipMed==null?-999.0f:(float)precipMed.getMean());
				precipMeanHighArr.setFloat(precipMeanHighArr.getIndex().set(ind1), precipHigh==null?-999.0f:(float)precipHigh.getMean());
				precipMeanVeryHighArr.setFloat(precipMeanVeryHighArr.getIndex().set(ind1), precipVeryHigh==null?-999.0f:(float)precipVeryHigh.getMean());
								
				guageRatioMeanLowArr.setFloat(guageRatioMeanLowArr.getIndex().set(ind1), guageRatioLow==null?-999.0f:(float)guageRatioLow.getMean());
				guageRatioMeanMedArr.setFloat(guageRatioMeanMedArr.getIndex(), guageRatioMed==null?-999.0f:(float)guageRatioMed.getMean());
				guageRatioMeanHighArr.setFloat(guageRatioMeanHighArr.getIndex(), guageRatioHigh==null?-999.0f:(float)guageRatioHigh.getMean());
				guageRatioMeanVeryHighArr.setFloat(guageRatioMeanVeryHighArr.getIndex(), guageRatioVeryHigh==null?-999.0f:(float)guageRatioVeryHigh.getMean());
			
				
			}
			// fake an array for writing out scalar data
			short mrmsPresent = 1;
			ArrayShort.D0 scalarData = new ArrayShort.D0();
			scalarData.set(mrmsPresent);
			// write vars
			mFptr.write(mrmsFlagVar, scalarData);
			mFptr.write(precipMeanLowVar, precipMeanLowArr);
			mFptr.write(precipMeanMedVar, precipMeanMedArr);
			mFptr.write(precipMeanHighVar, precipMeanHighArr);
			mFptr.write(precipMeanVeryHighVar, precipMeanVeryHighArr);
			
			mFptr.write(guageRatioMeanLowVar, guageRatioMeanLowArr);
			mFptr.write(guageRatioMeanMedVar, guageRatioMeanMedArr);
			mFptr.write(guageRatioMeanHighVar, guageRatioMeanHighArr);
			mFptr.write(guageRatioMeanVeryHighVar, guageRatioMeanVeryHighArr);

			mFptr.write(maskLowVar, maskLowArr);
			mFptr.write(maskMedVar, maskMedArr);
			mFptr.write(maskHighVar, maskHighArr);
			mFptr.write(maskVeryHighVar, maskVeryHighArr);

			mFptr.write(rqiPercentLowVar, rqiPercentLowArr);
			mFptr.write(rqiPercentMedVar, rqiPercentMedArr);
			mFptr.write(rqiPercentHighVar, rqiPercentHighArr);
			mFptr.write(rqiPercentVeryHighVar, rqiPercentVeryHighArr);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (mrms==null) {
				System.out.println("No matching MRMS data, skipping file " + vnInputFilename);
			}
			else
				e.printStackTrace();
		} finally {
			try {
				mFptr.close();
				
				if (mrms!=null) {
					// need to save modified temp files to output path 
		//			java.nio.file.Files.copy(java.nio.file.Paths.get(temp.getTempFilename()), java.nio.file.Paths.get(vnOutputFilename),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					//or compress..
					//temp.zip(temp.getTempFilename(),vnOutputFilename);
					// remove temp files
		//			temp.deleteTemp();
	
					if (vnOutputFilename.endsWith(".gz")) {
						temp.zip(temp.getTempFilename(),vnOutputFilename);
					}
					else {
						java.nio.file.Files.copy(java.nio.file.Paths.get(temp.getTempFilename()), java.nio.file.Paths.get(vnOutputFilename),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
					System.out.println("Finished file " + vnInputFilename);
					
					// loop through DPR footprints
					ArrayList<LatLng> gpmLatLon = new ArrayList<>();
					ArrayList<Float> sfcPrecipRate = new ArrayList<>();
					for (int ind1=0;ind1<fpdim.getLength();ind1++) {
						gpmLatLon.add(new LatLng(dprLatitude.getDouble(ind1),dprLongitude.getDouble(ind1)));
//						sfcPrecipRate.add(PrecipRate.getFloat(PrecipRate.getIndex().set(0,ind1)));
						sfcPrecipRate.add(PrecipRateSurface.getFloat(ind1));
//						sfcPrecipRate.add(SurfPrecipTotRate.getFloat(ind1));
					}
					outputSiteImagery(siteLat, siteLon, mrms, gpmLatLon, sfcPrecipRate, vnOutputFilename, "");

				}
				
				temp.deleteTemp();				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		
	}
	Map<Integer, Integer> maskHistogram(ArrayList<Integer> value, ArrayList<Float> filter, double min, double max) {
		
		Map<Integer, Integer> maskMap = new HashMap<>();
		// initialze histogram bins to zero
		for (int ind1=0;ind1<HISTOGRAM_INDICES.length;ind1++) {
			maskMap.put(HISTOGRAM_INDICES[ind1], 0);
		}
		for (int ind1=0;ind1<value.size();ind1++){
//			if (filter.get(ind1)>=min&&filter.get(ind1)<max&&value.get(ind1)>=0) {
			if (filter.get(ind1)>=min&&filter.get(ind1)<max) {
				int val = value.get(ind1);
				if (val<0) val=-1;
				Integer cnt=maskMap.get(val);
				cnt++;
				maskMap.put(val,cnt);
			}
//			else {
//				System.out.println("filter val " + filter.get(ind1));
//			}
		}
		return maskMap;
	}
	void outputKMLFile(BoundingBox imageBounds,String imageFilePath, String xmlFilename)
	{
		String outString;
		File f = new File(imageFilePath);
		String imageFilename = f.getName();
		
		outString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		outString += "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n";
		outString += "  <Folder>\n";
		outString += "    <name>GPM Validation Network</name>\n";
		outString += "    <description></description>\n";
		outString += "    <GroundOverlay>\n";
		outString += "     <name>" + imageFilename + "</name>\n";
		outString += "      <description>GPM Near Surface Rain Rate</description>\n";
		outString += "      <Icon>\n";
		outString += "       <href>" + imageFilename + "</href>\n";
		outString += "      </Icon>\n";
		outString += "      <LatLonBox>\n";
		outString += "        <north>" + imageBounds.getNorth().getLatitude() + "</north>\n";
		outString += "        <south>" + imageBounds.getSouth().getLatitude() + "</south>\n";
		outString += "        <east>" + imageBounds.getEast().getLongitude() + "</east>\n";
		outString += "        <west>" + imageBounds.getWest().getLongitude() + "</west>\n";
		outString += "      </LatLonBox>\n";
		outString += "    </GroundOverlay>\n";
		outString += "  </Folder>\n";
		outString += "</kml>\n";
		
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(xmlFilename);
			fout.write(outString.getBytes());
			fout.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	void outputSiteImagery(float siteLat, float siteLon, Mrms mrms, ArrayList<LatLng> gpmLatLon, ArrayList<Float> sfcPrecipRate, String vnOutputFilename, String outputDirname) 
	{
		
		BufferedImage mrmsimage;
		FileOutputStream mrmsfout = null;
		BufferedImage gpmimage;
		FileOutputStream gpmfout = null;
		FileOutputStream mrmsfoutBinary = null;
		FileOutputStream gpmfoutBinary = null;
		try {
			// originally had site radius of 125km, changed to 175 to make image size > 256 for deep learning (allows crop to 256)
			mrmsimage = mrms.getPRECIPRATEdata().floatDataToImage(0.0f,60.0f, siteLat, siteLon, 175.0f,true,true);
			mrmsfout = new FileOutputStream(vnOutputFilename + ".mrms.col" + ".png" );
		    ImageIO.write(mrmsimage, "png", mrmsfout);
			mrmsfout.close();
			BoundingBox imageBounds = mrms.getPRECIPRATEdata().getImageBounds();
			outputKMLFile(imageBounds,vnOutputFilename + ".mrms.col" + ".png", vnOutputFilename + ".mrms.col" + ".kml");

			ByteBuffer mrmsBinary = mrms.getPRECIPRATEdata().getBinarySiteValues();
			mrmsfoutBinary = new FileOutputStream(vnOutputFilename + ".mrms" + ".bin" );	
			mrmsfoutBinary.write(mrmsBinary.array());
			mrmsfoutBinary.close();
						
			gpmimage = mrms.getPRECIPRATEdata().matchGPMToImage(0.0f,60.0f, siteLat, siteLon, 175.0f, gpmLatLon, (float)(DPR_FOOTPRINT)/2.0f,sfcPrecipRate,true,true);
			gpmfout = new FileOutputStream(vnOutputFilename + ".gpm.col" + ".png" );
		    ImageIO.write(gpmimage, "png", gpmfout);
			gpmfout.close();
			imageBounds = mrms.getPRECIPRATEdata().getImageBounds();
			outputKMLFile(imageBounds,vnOutputFilename + ".gpm.col" + ".png", vnOutputFilename + ".gpm.col" + ".kml");

			ByteBuffer gpmBinary = mrms.getPRECIPRATEdata().getBinarySiteValues();
			gpmfoutBinary = new FileOutputStream(vnOutputFilename + ".gpm" + ".bin" );	
			gpmfoutBinary.write(gpmBinary.array());
			gpmfoutBinary.close();	
			
			mrmsimage = mrms.getPRECIPRATEdata().floatDataToImage(0.0f,60.0f, siteLat, siteLon, 175.0f,false,false);
			mrmsfout = new FileOutputStream(vnOutputFilename + ".mrms.bw" + ".png" );
		    ImageIO.write(mrmsimage, "png", mrmsfout);
			mrmsfout.close();
			imageBounds = mrms.getPRECIPRATEdata().getImageBounds();
			outputKMLFile(imageBounds,vnOutputFilename + ".mrms.bw" + ".png", vnOutputFilename + ".mrms.bw" + ".kml");

			gpmimage = mrms.getPRECIPRATEdata().matchGPMToImage(0.0f,60.0f, siteLat, siteLon, 175.0f, gpmLatLon, (float)(DPR_FOOTPRINT)/2.0f,sfcPrecipRate,false,false);
			gpmfout = new FileOutputStream(vnOutputFilename + ".gpm.bw" + ".png" );
		    ImageIO.write(gpmimage, "png", gpmfout);
			gpmfout.close();
			imageBounds = mrms.getPRECIPRATEdata().getImageBounds();
			outputKMLFile(imageBounds,vnOutputFilename + ".gpm.bw" + ".png", vnOutputFilename + ".gpm.bw" + ".kml");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	float percentAbove(ArrayList<Float> filter, double min, double max)
	{
		if (filter.size()==0) {
//			System.out.println("empty arraylist");
			return 0.0f;
		}
		int cnt=0;
		for (int ind1=0;ind1<filter.size();ind1++){
			if (filter.get(ind1)>=min&&filter.get(ind1)<max) {
				cnt++;
			}
		}
		return (100.0f*((float)cnt/(float)filter.size()));
		
	}
	DescriptiveStatistics filteredStats(ArrayList<Float> value, ArrayList<Float> filter, double min, double max) 
	{
		if (value.size()==0) {
//			System.out.println("empty arraylist");
			return null;
		}
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int ind1=0;ind1<value.size();ind1++){
			if (filter.get(ind1)>=min&&filter.get(ind1)<max&&value.get(ind1)>=0) {
				stats.addValue(value.get(ind1));
	//			System.out.println("filter " + filter.get(ind1));
			}
//			else {
//				System.out.println("filter val " + filter.get(ind1) + " value " + value.get(ind1));
//			}
		}
		if (stats.getN()==0)
			return null;
		else
			return stats;
		
		// Compute some statistics
//		double mean = stats.getMean();
//		double std = stats.getStandardDeviation();
//		double median = stats.getPercentile(50);
//		int count = (int) stats.getN();
		
	}
}
