package me.ziccard.secureit.async;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import me.ziccard.secureit.MonitorActivity;
import me.ziccard.secureit.config.Remote;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AuthenticatorTask extends AsyncTask<Void, Void, Void>{
	
	/**
	 * Reference activity
	 */
	private Activity activity;
	
	/**
	 * Username
	 */
	private String username;
	
	/**
	 * Password
	 */
	private String password;
	
	/**
	 * Progress dialog
	 */
	private ProgressDialog dialog;
	
	/**
	 * Authentication token
	 */
	private String accessToken;
	
	/**
	 * True iff authentication has failed
	 */
	private Boolean authenticationFailure = false;
	
	/**
	 * Manager to get device infos
	 */
	private TelephonyManager manager = (TelephonyManager) activity.
			getSystemService(Context.TELEPHONY_SERVICE);
	
	public AuthenticatorTask(Activity activity, String username, String password) {
		this.activity = activity;
		this.username = username;
		this.password = password;
	}
	
	/**
	 * Shows an authentication failure dialog
	 */
	public void showFailureDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage("Authentication failed")
				  .setCancelable(false)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {					

						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	
	@Override
	protected void onPreExecute() {
    	dialog = new ProgressDialog(activity);
    	dialog.setCancelable(false);
    	dialog.setTitle("Authenticating");
    	dialog.show();
	}

	@Override
	protected Void doInBackground(Void... params) {
		/*
		 * PHASE1: Authenticate
		 */
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(Remote.HOST+"signup");
		try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("email", username));
	        nameValuePairs.add(new BasicNameValuePair("password", password));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response;
				response = httpclient.execute(httppost);

		        
		        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
		        	authenticationFailure = true;
		        	return null;
		        }
		        accessToken = org.apache.http.util.EntityUtils.toString(response.getEntity());
		        Log.i("AuthenticatorTask", accessToken);
	        
			} catch (ClientProtocolException e) {
				authenticationFailure = true;
				e.printStackTrace();
			} catch (IOException e) {
				authenticationFailure = true;
				e.printStackTrace();
			}
		/*
		 * TODO retrieve access token
		 */
		/*
		 * PHASE2: Register phone
		 */
		httppost = new HttpPost(Remote.HOST+"phones");
		try {
	        
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("id", 
	        		manager.getDeviceId()));
	        nameValuePairs.add(new BasicNameValuePair("model", 
	        		Build.MANUFACTURER+" "+Build.PRODUCT));
	        nameValuePairs.add(new BasicNameValuePair("version", "VERSION"));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        
	        httppost.addHeader("access_token", accessToken);

	        // Execute HTTP Post Request
	        HttpResponse response;
			response = httpclient.execute(httppost);

		        
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
		    	authenticationFailure = true;
		    	return null;
		    }
		    accessToken = org.apache.http.util.EntityUtils.toString(response.getEntity());
		    Log.i("AuthenticatorTask", accessToken);
	        
		} catch (ClientProtocolException e) {
			authenticationFailure = true;
			e.printStackTrace();
		} catch (IOException e) {
			authenticationFailure = true;
			e.printStackTrace();
		}    
	    return null;
	        	        
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		dialog.dismiss();
		if (!authenticationFailure) {
			Intent intent = new Intent(
					activity,
					MonitorActivity.class);
			activity.startActivity(intent);
		} else {
			showFailureDialog();
		}
	}

}
