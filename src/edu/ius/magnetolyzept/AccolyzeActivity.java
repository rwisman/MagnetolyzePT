/*
 *     Copyright (C) 2014  Raymond Wisman
 * 			Indiana University SE
 * 			March 3, 2014
 * 
 * 	MagnetolyzePT - Magnetometer Collect and Analyze for Physics Toolbox Magnetometer.  

	The application is designed for use in science education experiments that:
		collect and analyze magnetometer data.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

  */

package edu.ius.magnetolyzept;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.view.Menu;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.view.View;

import java.io.BufferedReader;
import java.io.File; 
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import jxl.*; 
import jxl.write.*;
import jxl.write.Number;

public class AccolyzeActivity extends Activity {
	private final Activity activity = this;
	BufferedReader br = null;
	private File maxFile=null, xlsDir=null, accDir=null, copyFile=null, inFile=null;
	private boolean dismissed = false;
	private static final String DIRECTORY = "PhysicsToolboxMagnetometer";
	private static final String PROGRAM = "Physics Toolbox Magnetometer";
	private static final String PACKAGENAME="com.chrystianvieyra.android.physicstoolboxmagnetometer";
	private static String APP;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accolyze);
		APP = getString(R.string.app_name);	

		((Button) findViewById(R.id.buttonCollect)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				collect();
			}
		});
				
		((Button) findViewById(R.id.buttonAnalyze)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {   
				analyze();
				generateXLSDialog();
			}
		});
	}

	public void generateXLSDialog() {
		final ProgressDialog dialog = ProgressDialog.show(this, "Please wait.",	"Generating XLS file...", true);
        dialog.setCanceledOnTouchOutside(true);
		dialog.setCancelable(true);
		dismissed = false;
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(final DialogInterface dialog) {
    			dismissed = true;
			}
		});
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					generateXLS();
				} catch (Exception e) {

				}
				dialog.dismiss();
			}
		}).start();
	}

    private void errorDialog(final String msg){
	    runOnUiThread(new Runnable()   {
	        public void run()  {
	          	AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
	          	alertDialog.setTitle("Error");
	          	alertDialog.setMessage(msg);
	          	alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
	          		new DialogInterface.OnClickListener() {
	          			public void onClick(DialogInterface dialog, int i) {}
	          	});
	          	alertDialog.show();
	        }
	    });    
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void copyAssets(File dest) {
		AssetManager am = getAssets();
		try {
		    InputStream is = am.open( "input.xls");
		    
		    dest.createNewFile();	// Create new file to copy into.

		    copyFile(is, dest);

		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	public static void copyFile(InputStream in, File dst) {
	    OutputStream out = null;
	    try {
	    	
	        out = new FileOutputStream(dst);

	        byte[] buffer = new byte[1024];
	        int read;
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        in.close();
	        in = null;
	        out.flush();
	        out.close();
	        out = null;
	    } catch (Exception e) {	    }
	}

	private void collect() {
    	Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(PACKAGENAME);
    	startActivity(LaunchIntent);		
	}
	
	private void analyze() {
		Uri uri=null;
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    if (!action.equals(Intent.ACTION_VIEW)) {						// App started directly, try to find Accelerometer data directory and most recent file.	    	
	    	try {
				String filepath = Environment.getExternalStorageDirectory().getPath();
				accDir = new File(filepath, DIRECTORY);
				if (!accDir.exists()) {
					AlertDialog alertDialog = new AlertDialog.Builder(this).create();
					alertDialog.setTitle("Error");
					alertDialog.setMessage(DIRECTORY + " directory does not exist. Run "+PROGRAM+" program.");
					alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) { }
					});
					alertDialog.show();
	    			return;
				}
				// Obtain list of files in the directory. listFiles() returns a list of File objects to each file found.
				File[] files = accDir.listFiles();
			
				if (files == null || files.length==0) {
					AlertDialog alertDialog = new AlertDialog.Builder(this).create();
					alertDialog.setTitle("Error");
					alertDialog.setMessage("No files in "+DIRECTORY+" directory. Run "+PROGRAM+" program.");
					alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) { }
					});
					alertDialog.show();
	    			return;
				}

				long maxAge = 0;
				maxFile = files[0];
				// Loop through all files
				for (File f : files ) {		// Get the last modified date. 
			   		long lastmodified = f.lastModified();

					if(lastmodified > maxAge) { 
			      		maxAge = lastmodified;
			      		maxFile = f;
			   		}
				}
				br = new BufferedReader(new FileReader(maxFile));
			}
			catch(Exception e1) {}
	    }
	    else {
	    	uri = intent.getData();										// Started from another app
			ContentResolver cr = getContentResolver();	
			try {
				br = new BufferedReader(new InputStreamReader(cr.openInputStream(uri)));
			}
			catch(Exception e) {
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Error");
				alertDialog.setMessage("File cannot be opened: "+uri.toString());
				alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) { }
					});
				alertDialog.show();
				e.printStackTrace();
			}
	    }
	    
		try {
			String filepath = Environment.getExternalStorageDirectory().getPath();
			xlsDir = new File(filepath, APP);		
			
			if(!xlsDir.exists()) xlsDir.mkdirs();
			copyFile = new File(xlsDir.getAbsolutePath() + "/" + "copy.xls");
			inFile = new File(xlsDir.getAbsolutePath() + "/" + "input.xls");
			if (!inFile.exists())	
				copyAssets(inFile);	
			
			if (!inFile.exists()) {	
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Error");
				alertDialog.setMessage(APP + " directory must contain 'input.xls' spreadsheet to combine with "+PROGRAM+" data, creating 'input.xls'.");
				alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) { }
				});
				alertDialog.show();
				return;
			}

			for( int i=0; i<2; i++) br.readLine();								// First two lines are blank or comment

			if (copyFile.exists())
				copyFile.delete();
		}
		catch(Exception e){
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Error");
			alertDialog.setMessage(APP+" directory must contain 'input.xls' spreadsheet to combine with "+PROGRAM+" data, creating 'copy.xls'.");
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int i) { }
			});
			alertDialog.show();
			e.printStackTrace();
		}
	}
	
    private void generateXLS() {
    	String values[];
		try {
			Workbook inworkbook = Workbook.getWorkbook(inFile);

			WritableWorkbook copyworkbook = Workbook.createWorkbook(copyFile, inworkbook);
			
			WritableSheet sheet = copyworkbook.getSheet(0);  
			
			int row=1;
			Number number;
			double d;
			String line;
			
			while((line=br.readLine()) != null) {
				values =line.split("\\s*(;|,)\\s*");
				if(values.length > 1){
					number = new Number(0, row, row); 
					sheet.addCell(number);
					for(int col=1; col<5; col++) {
						d = Double.parseDouble(values[col]);
						number = new Number(col, row, d); 
						sheet.addCell(number);
					}
					row++;
				}
			}
			
			if( !dismissed ) {
				copyworkbook.write(); 
				copyworkbook.close();
				inworkbook.close();
				
			    MimeTypeMap map = MimeTypeMap.getSingleton();
			    String ext = MimeTypeMap.getFileExtensionFromUrl(copyFile.getName());
			    String type = map.getMimeTypeFromExtension(ext);
	
			    if (type == null)
			        type = "*/*";
	
			    Intent intent = new Intent(Intent.ACTION_VIEW);
			    Uri data = Uri.fromFile(copyFile);
	
			    intent.setDataAndType(data, type); 
	
			    startActivity(intent);	
			}
		}
		catch(Exception e) {
			errorDialog(PROGRAM+" file incorrectly formatted.");
		}	
	}
}
