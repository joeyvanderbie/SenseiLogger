package org.hva.sensei.sensors;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.hva.sensei.logger.MainActivity;

import android.os.AsyncTask;

public class UDPThread  extends AsyncTask<String, Void, Void> {
		
		String msensordata;

		@Override
		protected Void doInBackground(String... params) {
			byte bytes [] ;
			msensordata = params[0];
			
			try {
				bytes = msensordata.getBytes("UTF-8");
				if (MainActivity.mPacket == null || MainActivity.mSocket == null)
					return null ;
				
				MainActivity.mPacket.setData(bytes);
				MainActivity.mPacket.setLength(bytes.length);


				MainActivity.mSocket.send(MainActivity.mPacket);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//Log.e("Error", "SendBlock");
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//Log.e("Error", "SendBlock");
				return null;
			}
			return null;
		}
	}