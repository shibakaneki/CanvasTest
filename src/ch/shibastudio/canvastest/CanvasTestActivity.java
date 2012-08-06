package ch.shibastudio.canvastest;

import android.app.Activity;
import android.os.Bundle;

public class CanvasTestActivity extends Activity {
    private DrawingCanvas mCanvas;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCanvas = new DrawingCanvas(this);
        
        setContentView(mCanvas);
    }
}