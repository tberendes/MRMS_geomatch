package Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.*;

public class TempFile {
	private String fullFilename;
	private String filename;
	private File temp;
	private String tempFilename;
	private String tempDir=null;
	
	public String getTempFilename() {
		return tempFilename;
	}
	public File getFile()
	{
		return temp;
	}
	public TempFile(String fn, String tmpDir) throws IOException {
		tempDir = tmpDir;
		init(new File(fn));
	}  
	public TempFile(String fn) throws IOException {
		init(new File(fn));
	}  
	public TempFile(File f) throws IOException {
		init(f);
	}
	public TempFile() throws IOException {
		if (tempDir!=null)
			temp = File.createTempFile("tempfile_", null, new File(tempDir));
		else 
			temp = File.createTempFile("tempfile_", null);
		tempFilename = temp.getCanonicalFile().toString();
	}
	private void init(File f) throws IOException {
		filename=f.getName();
		fullFilename = f.getCanonicalFile().toString();
		if (tempDir!=null)
			temp = File.createTempFile("tempfile_", null, new File(tempDir));
		else 
			temp = File.createTempFile("tempfile_", null);
		tempFilename = temp.getCanonicalFile().toString();
	}  
	public void unzip() throws IOException {
		InputStream in = new FileInputStream(fullFilename);
		OutputStream out = new FileOutputStream(temp);
		unzip(in, out);
	}
	public void unzip(InputStream in) throws IOException {
		OutputStream out = new FileOutputStream(temp);
		unzip(in, out);
	}
	public void unzip(InputStream in, OutputStream out) throws IOException {
		try {
			in = new GZIPInputStream(in);
			byte[] buffer = new byte[65536];
			int numbytes;
			while ((numbytes = in.read(buffer)) != -1) {
				out.write(buffer, 0, numbytes);
			}
		} catch (IOException e) {
			System.out.println("IOException writing tempfile " + tempFilename);
		} finally {
			try { out.close(); } catch (Exception e) {
				System.out.println("Error closing file " + tempFilename);
			}
			try { in.close(); } catch (Exception e) {
				System.out.println("Error closing file " + filename);
			}
		}
	}
	public void zip() throws IOException {
		zip(fullFilename, tempFilename);
	}
	
	public void zip(String inFilename, String outFilename) throws IOException {
//		InputStream in = new FileInputStream(fullFilename);
//		OutputStream out = new FileOutputStream(temp);
		InputStream in = new FileInputStream(inFilename);
		OutputStream out = new FileOutputStream(outFilename);
		try {
			out = new GZIPOutputStream(out);
			byte[] buffer = new byte[65536];
			int numbytes;
			while ((numbytes = in.read(buffer)) != -1) {
				out.write(buffer, 0, numbytes);
			}
		} finally {
			try { out.close(); } catch (Exception e) {
				System.out.println("Error closing file " + tempFilename);
			}
			try { in.close(); } catch (Exception e) {
				System.out.println("Error closing file " + filename);
			}
		}
	}
	public void deleteTemp() throws IOException	{
		temp.delete();
	}
	public void copy()
	{
		Path source = Paths.get(fullFilename);
		Path destination = Paths.get(tempFilename);
 
		try {
			Files.copy(source, destination,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void saveFile(String outfilename)
	{
		Path source = Paths.get(tempFilename);
		Path destination = Paths.get(outfilename);
 
		try {
			Files.copy(source, destination,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void saveFile()
	// save file as basename.gz or basename without .gz
	{
		// unzipped file without extension
		if (fullFilename.endsWith(".gz"))
			saveFile(fullFilename.replace(".gz", ""));
		else
			saveFile(fullFilename + ".gz");
	}
}

