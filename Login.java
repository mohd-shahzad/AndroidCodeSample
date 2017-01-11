package org.TrailHUB;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;


import org.constants.AppUtility;
import org.constants.Constant;
import org.customview.CustomEditText;
import org.customview.CustomTypefaceSpan;
import org.model.UserModel;
import org.serverrequest.MyCustomRequest;
import org.serverrequest.MySingleton;
import org.serverrequest.NetworkResponseListener;
import org.serverrequest.ResponseErrorListener;
import org.serverrequest.ResponseSuccessListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.HashMap;
import java.util.Map;

/*
* This class have methods
* to validate data before sending to server,
* have method to make login api call
* and perform action according to different view
* click event.*/

public class Login extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppUtility.changeStatusBarColor(Login.this);

        /*
        *This will send report to
        *Google Analytic whenever user
        * will visit to this screen */
        AppUtility.trackScreen("LoginScreen" ,Login.this);

        initView();

    }

   /*
   * This  method register
   * view with its listener.
   * */
    private void initView()
    {
        findViewById(R.id.signup_redirection).setOnClickListener(this);
        findViewById(R.id.login_button).setOnClickListener(this);
        findViewById(R.id.cross_icon).setOnClickListener(this);
        findViewById(R.id.sign_up_member_selection).setOnClickListener(this);
        findViewById(R.id.sign_up_org_selection).setOnClickListener(this);
        findViewById(R.id.forgot_redirection).setOnClickListener(this);
        findViewById(R.id.selection_view).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        makeUnderlineTextClickcable();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     *This method will receive call of
     * all button click*/
    @Override
    public void onClick(View view)
    {
        //Every view click event will be receive here
        //depends on view appropriate action will be performed
        switch (view.getId())
        {
            case R.id.signup_redirection :
                findViewById(R.id.selection_view).setVisibility(View.VISIBLE);
                break;

            case R.id.login_button :


                if(validateData())
                {
                    if(AppUtility.isNetworkAvailable(Login.this))
                    {

                        AppUtility.showProgress(Login.this ,getString(R.string.pls_wait));
                        new GetToken(Login.this).execute("");
                        AppUtility.hideKeyboard(Login.this, view.getWindowToken());
                    }
                   else
                    {
                       AppUtility.showToast(Login.this, getString(R.string.netw_tit), getString(R.string.network_connection));
                    }
                }
                break;

            case R.id.cross_icon :
                findViewById(R.id.selection_view).setVisibility(View.GONE);
                break;

            case R.id.sign_up_org_selection :
                startActivity(new Intent(Login.this, SignUpOrganization.class));
                finish();
                break;

            case R.id.sign_up_member_selection :
                  startActivity(new Intent(Login.this, SignupMember.class));
                  finish();
                break;

            case R.id.forgot_redirection :
                startActivity(new Intent(Login.this, ForgotPassword.class));
                finish();
                break;

        }
    }

    @Override
     public void onBackPressed() {

        RelativeLayout layout = (RelativeLayout)findViewById(R.id.selection_view);
        if(layout.getVisibility() == View.VISIBLE)
        {
            layout.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

    /*
    * This method makes
    * API call of login that will be
    * called on Login Button click.*/
    private void makecallToLoginApi(String deviceToken)
    {

        if(AppUtility.isNetworkAvailable(Login.this))
        {

            String url = Constant.BASE_URL+Constant.API_NAME.LOG_IN.getKey();
            MyCustomRequest<UserModel> request = new MyCustomRequest<UserModel>(url , Request.Method.POST ,UserModel.class ,getParameter(deviceToken)
                    ,new ResponseSuccessListener<UserModel>(networkResponseListener ,Constant.LOGIN_TAG) ,new ResponseErrorListener(networkResponseListener,Constant.LOGIN_TAG));
            MySingleton.getInstance(Login.this).addToRequestQueue(request);

        }
        else
        {
            AppUtility.showToast(Login.this, getString(R.string.netw_tit), getString(R.string.network_connection));
        }

    }

    /*
    * This listener receives response of server.
    * */
    NetworkResponseListener networkResponseListener = new NetworkResponseListener() {
        @Override
        public void onNetworkResponseSuccess(Object successResponse, String tag){
            //On successful response of server request
            //call will be received here.

            if(tag.equalsIgnoreCase(Constant.LOGIN_TAG))
            {
                AppUtility.hideProgress();

                performOperation((UserModel)successResponse);
            }

        }

        @Override
        public void onNetworkResponseFailure(VolleyError volleyError, String tag)
        {    //On unsuccessful response of server request with reason
            //call will be received here.

            AppUtility.hideProgress();
            Toast.makeText(Login.this ,getString(R.string.some_wrong) ,Toast.LENGTH_SHORT).show();
        }
    };

    /*
    * This method performs operation after receiving
    * response from server*/
    private void performOperation(UserModel model)
    {
        if(model != null)
        {
            if(model.status.equalsIgnoreCase(Constant.SUCCESS))
            {
                CustomEditText userNameView = (CustomEditText)findViewById(R.id.login_username);
                CustomEditText passwordView = (CustomEditText)findViewById(R.id.login_password);
                model.email = userNameView.getText().toString();
                AppUtility.storeUserModel(Login.this ,model);

                SharedPreferences pref = getSharedPreferences(Constant.SHARED_PREF ,MODE_PRIVATE);
                pref.edit().putBoolean(Constant.IS_LOGIN ,true).commit();

                AppUtility.storeUserInfo(Login.this ,model ,userNameView.getText().toString()
                        ,passwordView.getText().toString());
                startActivity(new Intent(Login.this, SearchOrganization.class));

                finish();


            }
            else if(model.status.equalsIgnoreCase(Constant.ERROR))
            {
                AppUtility.showToast(Login.this, getString(R.string.error_tit), getString(R.string.login_fail));

            }
            else
            {
                AppUtility.showToast(Login.this, getString(R.string.error_tit), getString(R.string.some_wrong));

            }
        }
        else
        {
            AppUtility.showToast(Login.this, getString(R.string.error_tit), getString(R.string.some_wrong));
        }

    }

    /*
     *This method prepares map
    * having parameter to send login API call*/
    private Map<String ,String> getParameter(String deviceToken)
    {
        String userName = ((TextView)findViewById(R.id.login_username)).getText().toString();
        String password = ((TextView)findViewById(R.id.login_password)).getText().toString();
        String androidId = Settings.Secure.getString(getContentResolver(),
                  Settings.Secure.ANDROID_ID);

        Map<String ,String> map = new HashMap<String ,String>();
        map.put(Constant.COMMON_API_KEYS.EMAIL.getKey() ,userName);
        map.put(Constant.COMMON_API_KEYS.PASSWORD.getKey() ,password);
        map.put(Constant.COMMON_API_KEYS.DEVICETYPE.getKey() ,"A");
        map.put(Constant.COMMON_API_KEYS.DEVICETOKEN.getKey(), deviceToken);
        map.put(Constant.COMMON_API_KEYS.DEVICE_ID.getKey() ,androidId);

        SharedPreferences pref = getSharedPreferences(Constant.SHARED_PREF ,MODE_PRIVATE);
        pref.edit().putString(Constant.PF_DEVICE_TOKEN ,androidId).commit();

        return map;

    }

    /*
      *This method validates data
      * enter by user before
      *sending to server.
      * */
    private boolean validateData()
    {
        CustomEditText userNameView = (CustomEditText)findViewById(R.id.login_username);
        CustomEditText passwordView = (CustomEditText)findViewById(R.id.login_password);


        String userName = userNameView.getText().toString().trim();
        String password = passwordView.getText().toString().trim();

        if(userName.equalsIgnoreCase(""))
        {
            AppUtility.showToast(Login.this, getString(R.string.valid_tit), getString(R.string.empty_usrname));
           return false;

        }
        else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(userName).matches())
        {
            AppUtility.showToast(Login.this, getString(R.string.valid_tit), getString(R.string.invalid_usrname));
            return false;
        }
        else if(password.equalsIgnoreCase(""))
        {
            AppUtility.showToast(Login.this, getString(R.string.valid_tit), getString(R.string.empty_pswd));
            return false;
        }
        else if(password.length()<4)
        {
            AppUtility.showToast(Login.this, getString(R.string.valid_tit), getString(R.string.invalid_password));
            return false;
        }
        else
        {
            return true;
        }


    }


    /*
    * This method makes underline
    * Sign Up text clickable*/
    private void makeUnderlineTextClickcable()
    {
        String str = getString(R.string.login_not_member_txt);
        int i1 = str.indexOf("Si");
        int i2 = str.indexOf("Up");

        TextView textview = (TextView)findViewById(R.id.complete_txt);
        Typeface font = Typeface.createFromAsset(getAssets(), getString(R.string.font_normal));

        Spannable SS = new SpannableString(str);
        SS.setSpan (new CustomTypefaceSpan("", font), 0, i1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

        textview.setText(SS);

        textview.setMovementMethod(LinkMovementMethod.getInstance());
        Spannable mySpannable = (Spannable)textview.getText();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view)
            {
                findViewById(R.id.selection_view).setVisibility(View.VISIBLE);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);

                Typeface font1 = Typeface.createFromAsset(getAssets(), getString(R.string.font_bold));
                CustomTypefaceSpan.applyCustomTypeFace(ds, font1);
            }
        };

        mySpannable.setSpan(clickableSpan, i1, i2 + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    }


   /*
    * This class will use to send project number to GCM server
    * and device token will be receive from GCM server*/
    private class GetToken extends AsyncTask<String ,String ,String>{

        private Context context;

        public GetToken(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(String... strings) {
            String token = null;
            try {

                InstanceID instanceID = InstanceID.getInstance(context);

                token = instanceID.getToken(Constant.GCM_PROJECT_NUMBER,
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);


            } catch (Exception e)
            {

                e.printStackTrace();
            }


            return token;
        }


        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);


            if(token!=null && !token.equalsIgnoreCase(""))
            {

                // if token is received
                //login api is called.
                makecallToLoginApi(token);
            }
            else
            {
                AppUtility.hideProgress();
                AppUtility.showToast(Login.this ,"",getString(R.string.token_unavail));
            }

        }
    }


}
