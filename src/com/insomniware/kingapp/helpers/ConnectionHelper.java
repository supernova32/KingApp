package com.insomniware.kingapp.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.widget.Toast;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import com.insomniware.kingapp.MainPageActivity;

import android.util.Log;

public class ConnectionHelper {
	
	String mainUrl = "https://kingapp.herokuapp.com/api/v1/";
	JSONObject result = new JSONObject();
	DefaultHttpClient httpclient = new DefaultHttpClient();
	HttpPost httppostreq;
	StringEntity se;
    Context context;
	
	public ConnectionHelper(String path, JSONObject jsonobj){
		httppostreq = new HttpPost(mainUrl+path);
		try {
			jsonobj.put("auth_token", MainPageActivity.auth_token);
			se = new StringEntity(jsonobj.toString());
			se.setContentType("application/json;charset=UTF-8");
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
			httppostreq.setEntity(se);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
	
	public JSONObject performRequest(Context context) throws IOException {

        this.context = context;
		
		try {
			Log.e("Connecting to", mainUrl);
			HttpResponse httpresponse = httpclient.execute(httppostreq);
			HttpEntity resultentity = httpresponse.getEntity();
			InputStream inputstream = resultentity.getContent();
			Header contentencoding = httpresponse.getFirstHeader("Content-Encoding");
			if(contentencoding != null && contentencoding.getValue().equalsIgnoreCase("gzip")) {
				inputstream = new GZIPInputStream(inputstream);
			}
			String resultstring = convertStreamToString(inputstream);
			Log.e("JSON Received", resultstring);
			inputstream.close();
			result = new JSONObject(resultstring);
		} catch (JSONException e) {
            Toast.makeText(context, "Stream Exception", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}	
		
		return result;		
		
	}
	
	private String convertStreamToString(InputStream is) {
	    String line = "";
	    StringBuilder total = new StringBuilder();
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    try {
	        while ((line = rd.readLine()) != null) {
	            total.append(line);
	        }
	    } catch (Exception e) {
	    	Toast.makeText(context, "Stream Exception", Toast.LENGTH_SHORT).show();
	    }
	    return total.toString();
	}

}
