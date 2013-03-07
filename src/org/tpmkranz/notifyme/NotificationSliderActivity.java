package org.tpmkranz.notifyme;

/*	Notify Me!, an app to enhance Android(TM)'s abilities to show notifications.
	Copyright (C) 2013 Tom Kranz
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
	
	Android is a trademark of Google Inc.
*/
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.RemoteViews;

public class NotificationSliderActivity extends Activity {

	Prefs prefs;
	int filter;
	boolean big, showPopup, touchValid, triggers;
	Notification notif;
	View nView;
	ViewGroup pView;
	SliderSurfaceView sView;
	float X, lastX;
	DrawTask dTask;
	GestureDetector geDet;
	AlertDialog dialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setFlags(LayoutParams.FLAG_TURN_SCREEN_ON, LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().setFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED, LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		prefs = new Prefs(this);
		filter = ((TemporaryStorage)getApplicationContext()).getFilter();
		dTask = new DrawTask();
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume(){
		super.onResume();
		big = false;
		notif = (Notification) ((TemporaryStorage)getApplicationContext()).getParcelable();
		showPopup();
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void showPopup(){
		RemoteViews remViews = null;
		showPopup = prefs.isPopupAllowed(filter);
		try{
			if( big ){
				remViews = notif.bigContentView;
			}else{
				remViews = notif.contentView;
			}
		}catch(Exception e){
			showPopup = false;
		}
		if( !((PowerManager)getSystemService(POWER_SERVICE)).isScreenOn() )
			showPopup = false;
		if( showPopup ){
			pView = (ViewGroup) ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.slider_popup, null);
			nView = remViews.apply(this, pView);
			pView.addView(nView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			sView = (SliderSurfaceView) pView.getChildAt(1);
			geDet = new GestureDetector( new UnlockListener() );
			sView.setOnTouchListener(
				new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if( event.getAction() == MotionEvent.ACTION_UP ){
							touchValid = false;
						}
						return geDet.onTouchEvent(event);
					}
				}
			);
			sView.setOnClickListener(null);
			sView.setZOrderOnTop(true);
			dialog = new AlertDialog.Builder(this).setView(pView).setInverseBackgroundForced(prefs.isBackgroundColorInverted()).create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						finish();
					}
				}
			);
			dTask.cancel(true);
			dTask = new DrawTask();
			dTask.execute();
		}else{
			dialog = new AlertDialog.Builder(this).create();
		}
		dialog.show();
		if( !showPopup ){
			dialog.dismiss();
		}
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		dialog.dismiss();
		dTask.cancel(true);
	}
	
	@Override
	protected void onNewIntent(Intent intent){
		if( !getIntent().equals(intent) ){
			finish();
			startActivity(intent);
		}else
			super.onNewIntent(intent);
	}
	
	private class DrawTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			while( sView.getWidth() == 0 ){
				
			}
			sView.setDimensions((float) sView.getWidth(),(float) sView.getHeight());
			Resources res = getResources();
			sView.setBitmaps(BitmapFactory.decodeResource(res, R.drawable.ic_lock_lock),
					BitmapFactory.decodeResource(res, R.drawable.ic_lock_handle),
					BitmapFactory.decodeResource(res, R.drawable.ic_lock_view),
					BitmapFactory.decodeResource(res, R.drawable.ic_lock_view0),
					BitmapFactory.decodeResource(res, R.drawable.ic_lock_dismiss),
					BitmapFactory.decodeResource(res, R.drawable.ic_lock_dismiss0));
			X = sView.centerX;
			lastX = X / 2;
			while( !this.isCancelled() ){
				while( sView.onDisplay ){
					if( lastX != X ){
						sView.doDraw(X, touchValid);
						if( !touchValid ){
							lastX = X;
							X = sView.centerX;
						}
					}
				}
			}
			return null;
		}
	}
	
	private class UnlockListener extends SimpleOnGestureListener{
		float[] a = new float[2];
		float[] b = new float[2];
		
		@Override
		public boolean onDown(MotionEvent ev){
			a[0] = ev.getX();
			a[1] = ev.getY();
			b[0] = sView.centerX;
			b[1] = sView.centerY;
			if( sView.dist(a, b) < sView.offsetY ){
				touchValid = true;
				triggers = true;
			}
			return touchValid;
		}
		
		@Override
		public boolean onScroll(MotionEvent ev1, MotionEvent ev2, float dX, float dY){
			if( Math.abs(ev2.getY() - sView.centerY)/(Math.abs(ev2.getX() - sView.centerX) + sView.offsetX)  >= 1 ){
				touchValid = false;
				triggers = false;
			}
			if( touchValid ){
				lastX = X;
				X = ev2.getX();
				return true;
			}else
				return false;
		}
		
		@Override
		public void onLongPress(MotionEvent ev){
		}
		
		@SuppressLint("NewApi")
		@Override
		public boolean onFling(MotionEvent ev1, MotionEvent ev2, float vX, float vY){
/*			a[0] = ev1.getX();
			a[1] = ev1.getY();
			b[0] = sView.centerX;
			b[1] = sView.centerY;*/
			if( sView.dist(a, b) < sView.offsetY ){
				if( ev2.getX() <= sView.leftX && triggers )
					dialog.cancel();
				else if( ev2.getX() >= sView.rightX && triggers ){
					dialog.cancel();
					startActivity(new Intent(getApplicationContext(), Unlock.class));
				}else if( android.os.Build.VERSION.SDK_INT >= 16 && !triggers && Math.abs(vY) > 4 * getResources().getDisplayMetrics().densityDpi && prefs.isExpansionAllowed(filter) ){
					try{
						notif.bigContentView.hashCode();
						if( vY > 0 && big || vY < 0 && !big )
							return true;
						dialog.dismiss();
						big = !big;
						showPopup();
					}catch(Exception e){
						
					}
				}
				return true;
			}else{
				return false;
			}
		}
	}
}