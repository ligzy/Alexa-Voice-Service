package com.game.dhanraj.myownalexa;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.game.dhanraj.myownalexa.AccessConstant.CodeVerifierandChallengeMethods;
import com.game.dhanraj.myownalexa.Alarm.MyAlarm;
import com.game.dhanraj.myownalexa.NavigationDrawer.NavigationFragment;
import com.game.dhanraj.myownalexa.sharedpref.Util;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.game.dhanraj.myownalexa.AccessConstant.CodeVerifierandChallengeMethods.generateCodeChallenge;


//The authorization code changes everytime you open the app for the first time so you have to get the authorization code everytime you login till your access
public class MainActivity extends AppCompatActivity {
    
    
    //Fill in the Device type ID here or Application type ID
    private static final String PRODUCT_ID = "DhanrajCompanionProduct";

    public TokenHandler tokenHanlder;
    private RequestContext mRequestContext;
    public static Context myContext;
    private static String PRODUCT_DSN;
    private static String CODE_CHALLENGE_METHOD = "S256";
    private String codeVerifier;
    private static String authCode, redirectURI, clientID;
    private static String codeChallenge;

    private TextView txt;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private CircleImageView Loginbtn;
    private Toolbar toolbar;

    private static String myresponse;
    public static String CLIENTID;

    public static DownChannel dwn;

    public final static String PREF_ACCESS_TOKEN = "access_token_042017";
    public final static String PREF_REFRESH_TOKEN = "refresh_token_042017";
    public final static String PREF_TOKEN_EXPIRES = "token_expires_042017";

    private ProgressDialog progressDialog;
    private static boolean forDownChannel,refreshToken;
    public static boolean CheckInternetConnection,DownChannelestablished;

    //private SharedPreferences preferences;
    SharedPreferences validation;
    SharedPreferences.Editor editorValidation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_app_bar);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myContext = MainActivity.this;

        mRequestContext = RequestContext.create(this);
        mRequestContext.registerListener(new AuthorizeListenerImpl());

        txt = (TextView) findViewById(R.id.text);
        PRODUCT_DSN = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        try {
            codeVerifier = CodeVerifierandChallengeMethods.generateCodeVerifier();
            codeChallenge = generateCodeChallenge(codeVerifier, "S256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Your JRE does not support the required "
                    + "SHA-256" + " algorithm.", e);
        }

        refreshToken= true;
        DownChannelestablished = true;
        dwn = new DownChannel();
        tokenHanlder = new TokenHandler(myContext);

        Loginbtn = (CircleImageView) findViewById(R.id.btn);
        Loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager connectivityManager
                        = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if(activeNetworkInfo != null && activeNetworkInfo.isConnected())
                intiLogi();
                else
                {
                    Snackbar.make(v, "No Internet Connectivity", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        NavigationFragment navigationFragment = (NavigationFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_fragment);
            navigationFragment.setUp(R.id.navigation_fragment,(DrawerLayout)findViewById(R.id.drawer_layout),toolbar);
        }


    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        /*ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (!(activeNetworkInfo != null && activeNetworkInfo.isConnected())) {

        }else
        {
            SharedPreferences preferences= Util.getPrefernces(myContext);

            if (preferences.contains(PREF_REFRESH_TOKEN)) {
                Intent st = new Intent(myContext,DownChannel.class);
                startService(st);
            }
        }*/
    }


    @Override
    protected void onResume() {
        super.onResume();
        mRequestContext.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(MessageEvent event){
        switch (event.event){
            case TokenHandler.FinishMainActivity:
                finish();
               break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
       /* Intent intent = new Intent(MainActivity.this, DownChannel.class);
        MainActivity.this.stopService(intent);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main2, menu);
        return true;
    }


    @Override
    public void onBackPressed() {
         DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
               /* Intent i = new Intent(MainActivity.this, MyAlarm.class);
                startActivity(i);*/
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void intiLogi() {

        final JSONObject scopeData = new JSONObject();
        final JSONObject productInstanceAttributes = new JSONObject();

        try {
            productInstanceAttributes.put("deviceSerialNumber", PRODUCT_DSN);
            scopeData.put("productInstanceAttributes", productInstanceAttributes);
            scopeData.put("productID", PRODUCT_ID);

            AuthorizationManager.authorize(new AuthorizeRequest.Builder(mRequestContext)
                    .addScope(ScopeFactory.scopeNamed("alexa:all", scopeData))
                    .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                    .withProofKeyParameters(codeChallenge, CODE_CHALLENGE_METHOD)
                    .build());
        } catch (JSONException e) {
            // handle exception here
        }
    }


    private class AuthorizeListenerImpl extends AuthorizeListener {

        /* Authorization was completed successfully. */
        @Override
        public void onSuccess(final AuthorizeResult authorizeResult) {
            final String authorizationCode = authorizeResult.getAuthorizationCode();
            final String redirectUri = authorizeResult.getRedirectURI();
            final String clientId = authorizeResult.getClientId();
            CLIENTID = clientId;

                    SharedPreferences.Editor  preferences = Util.getPrefernces(myContext).edit();
                    preferences.putString("clientID",clientId);
                    preferences.apply();
                    RequestBody formBody = new FormBody.Builder()
                            .add("grant_type", "authorization_code")
                            .add("code", authorizationCode)
                            .add("redirect_uri", redirectUri)
                            .add("client_id", clientId)
                            .add("code_verifier", codeVerifier)
                            .build();

                    tokenHanlder.doPostRequest(formBody,TokenHandler.FirstMainActivityDoPostRequest);

        }

        /* There was an error during the attempt to authorize the application. */
        @Override
        public void onError(final AuthError authError) {
            intiLogi();
        }

        /* Authorization was cancelled before it could be completed. */
        @Override
        public void onCancel(final AuthCancellation authCancellation) {
            Toast.makeText(MainActivity.this, "Your authorization has been cancelled. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
