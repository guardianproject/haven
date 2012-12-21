package me.ziccard.secureit.fragment;

import me.ziccard.secureit.Preview;
import me.ziccard.secureit.R;
import me.ziccard.secureit.async.MotionAsyncTask.MotionListener;
import me.ziccard.secureit.codec.ImageCodec;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.hardware.SensorEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;

public final class CameraFragment extends Fragment {
        
    private Preview preview;
    
    private ImageView oldImage;
    private ImageView newImage;
 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.camera_fragment, container, false);

	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    }
 
    @Override
    public void onPause() {
    	super.onPause();
    } 
    
    @Override
    public void onResume() {
    	super.onResume();
    	
		//Uncomment to see the camera
		preview = new Preview(getActivity());
		((FrameLayout) getActivity().findViewById(R.id.preview)).addView(preview);

        oldImage = (ImageView) getActivity().findViewById(R.id.old_image);
        newImage = (ImageView) getActivity().findViewById(R.id.new_image);
        
        preview.addListener(new MotionListener() {
			
			public void onProcess(Bitmap oldBitmap, Bitmap newBitmap,
					boolean motionDetected) {
				oldImage.setImageBitmap(ImageCodec.rotate(oldBitmap,90));
				newImage.setImageBitmap(ImageCodec.rotate(newBitmap,90));
				
			}
		});
        
    }

	public void onSensorChanged(SensorEvent event) {

	}
}