package parking.parkingapp;

import android.content.Context;
import android.webkit.WebView;
//
// This class is really a mini web browser
//
public class GifView extends WebView {

    public GifView(Context context, String path) {
        super(context);
        loadUrl(path);
    }
}
