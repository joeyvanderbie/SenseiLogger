package org.hva.sensei.sensors;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.hva.sensei.logger.MainMovementActivity;

import android.os.AsyncTask;

public class UDPThread  extends AsyncTask<String, Void, Void> {
		
		String msensordata;

		@Override
		protected Void doInBackground(String... params) {
			byte bytes [] ;
			msensordata = params[0];
			
			try {
				bytes = msensordata.getBytes("UTF-8");
				if (MainMovementActivity.mPacket == null || MainMovementActivity.mSocket == null)
					return null ;
				
				MainMovementActivity.mPacket.setData(bytes);
				MainMovementActivity.mPacket.setLength(bytes.length);


				MainMovementActivity.mSocket.send(MainMovementActivity.mPacket);
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