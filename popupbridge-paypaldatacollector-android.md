This is a code snippet demonstrating the collection of device data for PayPal when using PopupBridge for Android.

# MainActivity.java

```
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.PopupBridge;
import com.braintreepayments.popupbridge.PopupBridgeNavigationListener;
import com.paypal.android.sdk.data.collector.SdkRiskComponent;

public class MainActivity extends Activity implements PopupBridgeNavigationListener {

    private WebView mWebView;
    private PopupBridge mPopupBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.web_view);

        mPopupBridge = PopupBridge.newInstance(this, mWebView);

        mPopupBridge.setNavigationListener(this);
        mWebView.loadUrl("http://10.0.2.2");
    }

    @Override
    public void onUrlOpened(String url) {
        String id = "";
        Uri uri = Uri.parse(url);

        String checkoutId = uri.getQueryParameter("token");
        String vaultId = uri.getQueryParameter("ba_token");

        if (!TextUtils.isEmpty(checkoutId)) {
            id = checkoutId;
        } else if (!TextUtils.isEmpty(vaultId)) {
            id = vaultId;
        }

        Log.d("result", PayPalDataCollector.getClientMetadataId(this, id));
    }
}
```
