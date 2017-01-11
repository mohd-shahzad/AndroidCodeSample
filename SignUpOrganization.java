package org.TrailHUB;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;


import org.adapters.SignUpCountryAdapter;
import org.adapters.SignUpRegionListAdapter;
import org.adapters.SignUpStateAdapter;
import org.adapters.SignupActivityAdapter;
import org.adapters.SignupTownAdapter;
import org.constants.AppUtility;
import org.constants.Constant;
import org.customview.CustomEditText;
import org.customview.CustomTextView;
import org.customview.CustomTypefaceSpan;
import org.model.BaseModel;
import org.model.SignUpActivityModel;
import org.model.SignUpCountryListModel;
import org.model.UserModel;
import org.serverrequest.MyCustomRequest;
import org.serverrequest.MySingleton;
import org.serverrequest.NetworkResponseListener;
import org.serverrequest.ResponseErrorListener;
import org.serverrequest.ResponseSuccessListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* This class is used for
* Organization sign up  */

public class  SignUpOrganization extends FragmentActivity implements View.OnClickListener ,AdapterView.OnItemSelectedListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_organization);
        AppUtility.changeStatusBarColor(SignUpOrganization.this);

        //This will send report to
        //Google Analytic whenever user
        //will visit to this screen
        //Track Screen on google analytic
        AppUtility.trackScreen("SignUpOrganization Screen" ,SignUpOrganization.this);

        initView();
        makeTermsConditionsClickcable();
        makeTermsConditionCall();
    }

    /*
  * This  method register
  * view with its listener.
  * */
    private void initView()
    {

        AutoCompleteTextView town = (AutoCompleteTextView)findViewById(R.id.signup_org_town);

        findViewById(R.id.activity_label).setOnClickListener(this);
        findViewById(R.id.country_label).setOnClickListener(this);
        findViewById(R.id.state_label).setOnClickListener(this);
        findViewById(R.id.region_label).setOnClickListener(this);
        findViewById(R.id.signup_org_submit).setOnClickListener(this);

        ((Spinner)findViewById(R.id.signup_activity)).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.signup_country)).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.signup_state)).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.signup_region)).setOnItemSelectedListener(this);
        town.setTypeface(Typeface.createFromAsset(getAssets(), getString(R.string.font_normal)));

        town.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                if (keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    AppUtility.hideKeyboard(SignUpOrganization.this, textView.getWindowToken());
                }
                return false;
            }
        });
       makeUnderlineTextClickcable();
       makecallToSignupActivityListApi();
       makecallToSignupCountryListApi();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sign_up_organization, menu);
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


    @Override
    public void onClick(View view)
    {
      //Every view click event will be receive here
      //depends on view appropriate action will be performed
        switch (view.getId())
        {
            case R.id.activity_label:

                break;

            case R.id.country_label:

                break;

            case R.id.state_label:

                break;

            case R.id.signup_region:

                break;

            case R.id.signin_redirection:
                startActivity(new Intent(SignUpOrganization.this ,Login.class));
                finish();

                break;

            case R.id.signup_org_submit:

                if(validateData())
                {

                    AppUtility.hideKeyboard(SignUpOrganization.this, view.getWindowToken());
                     if(AppUtility.isNetworkAvailable(SignUpOrganization.this))
                    {

                        AppUtility.showProgress(SignUpOrganization.this ,getString(R.string.pls_wait));
                        new GetToken(SignUpOrganization.this).execute("");
                     }
                    else
                    {
                        AppUtility.showToast(SignUpOrganization.this, getString(R.string.netw_tit), getString(R.string.network_connection));
                    }

                }

                break;

        }

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
    {
        //Activity ,country ,state and region spinner
        // item selection event is received here.
        //On basis of country selection statelist is
        //set in state spinner ,on basis of state
        //selection region of that state is selected
        //in region spinner and on basis of region
        //selection town is set for suggestion in
        //Autocompleteviewof town.


        Spinner signupState = (Spinner)findViewById(R.id.signup_state);
        Spinner signupRegion = (Spinner)findViewById(R.id.signup_region);

        TextView stateLabel = (TextView)findViewById(R.id.state_label);
        TextView regionLabel = (TextView)findViewById(R.id.region_label);

       switch (adapterView.getId())
        {
            case R.id.signup_activity :

                break;

            case R.id.signup_country :

                if(i==0)
                {
                    stateLabel.setVisibility(View.VISIBLE);
                    signupState.setVisibility(View.GONE);
                    signupState.setAdapter(null);
                    regionLabel.setVisibility(View.VISIBLE);
                    signupRegion.setVisibility(View.GONE);
                    signupRegion.setAdapter(null);
                    return;
                }
                else
                {
                    stateLabel.setVisibility(View.GONE);
                    signupState.setVisibility(View.VISIBLE);
                }



                   SignUpCountryAdapter adp = (SignUpCountryAdapter)adapterView.getAdapter();
                   SignUpCountryListModel signUpCountryListModel = new SignUpCountryListModel();
                   SignUpCountryListModel.records recordsObject = signUpCountryListModel.new records();
                   SignUpCountryListModel.records.countryState countryStateObject = recordsObject.new countryState();

                    if(adp.getItem(i).countryId.equalsIgnoreCase("2"))
                    {
                        countryStateObject.stateName = getString(R.string.sel_pro);
                    }
                    else
                    {
                        countryStateObject.stateName = getString(R.string.sel_sta);
                    }

                   ArrayList<SignUpCountryListModel.records.countryState> arrayList
                         = new ArrayList<SignUpCountryListModel.records.countryState>();
                   arrayList.addAll(adp.getCountryList().get(i).countryState);
                    arrayList.add(0, countryStateObject);
                   SignUpStateAdapter adpS = new SignUpStateAdapter(SignUpOrganization.this ,arrayList);
                   findViewById(R.id.state_label).setVisibility(View.GONE);
                   signupState.setVisibility(View.VISIBLE);
                   signupState.setAdapter(adpS);



                break;

            case R.id.signup_state :



                if(i==0)
                {
                    regionLabel.setVisibility(View.VISIBLE);
                    signupRegion.setVisibility(View.GONE);
                    signupRegion.setAdapter(null);
                    return;
                }
                else
                {
                    regionLabel.setVisibility(View.GONE);
                    signupRegion.setVisibility(View.VISIBLE);
                }

                SignUpStateAdapter sadp = (SignUpStateAdapter)adapterView.getAdapter();
                SignUpCountryListModel signUpCountryListModel2 = new SignUpCountryListModel();
                SignUpCountryListModel.records recordsObject2 = signUpCountryListModel2.new records();
                SignUpCountryListModel.records.countryState countryStateObject2 = recordsObject2.new countryState();
                SignUpCountryListModel.records.countryState.regions region2 = countryStateObject2.new regions();
                region2.regionName="Select Region";

                 ArrayList<SignUpCountryListModel.records.countryState.regions> newRegion
                        = new ArrayList<SignUpCountryListModel.records.countryState.regions>();
                 newRegion.addAll(sadp.getStateList().get(i).regions);
                 newRegion.add(0,region2);


                SignUpRegionListAdapter sregAdp = new SignUpRegionListAdapter(SignUpOrganization.this ,newRegion);
                signupRegion.setAdapter(sregAdp);

                break;

            case R.id.signup_region :

                AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.signup_org_town);
                SignupTownAdapter signupTownAdapter = (SignupTownAdapter)autoCompleteTextView.getAdapter();
                SignUpRegionListAdapter signUpRegionListAdapter =(SignUpRegionListAdapter)adapterView.getAdapter();
                SignUpCountryListModel.records.countryState.regions regionObject = signUpRegionListAdapter.getItem(i);
                if(signupTownAdapter == null)
                {
                    if(regionObject.town != null && regionObject.town.size()>0)
                    {

                        autoCompleteTextView.setAdapter(new SignupTownAdapter(SignUpOrganization.this ,regionObject.town));
                    }

                }
                else
                {

                    if(regionObject.town != null && regionObject.town.size()>0)
                    {
                        signupTownAdapter.setTownList(regionObject.town);
                        signupTownAdapter.notifyDataSetChanged();
                    }
                    else
                    {

                        autoCompleteTextView.setAdapter(null);

                    }
                }

                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {

    }

    @Override
    public void onBackPressed() {

       //on device back button pressed
       //term and condition fragment is remove if it
       // is added.

        FragmentManager fragmentManager = getSupportFragmentManager();
        TermsAndConditionsFragment fragment = (TermsAndConditionsFragment)fragmentManager.findFragmentByTag(Constant.TERM_FRAGMENT_TAG);
        if(fragment != null)
        {
            fragmentManager.beginTransaction().remove(fragment).commit();
            return;
        }
        super.onBackPressed();
    }

    /*
    * It call an api to get
    * activity list to show in
    * activity spinner.
    * */
    private void makecallToSignupActivityListApi()
    {

        if(AppUtility.isNetworkAvailable(SignUpOrganization.this))
        {

            String url = Constant.BASE_URL+"getActivityList";
            Map<String ,String> map = new HashMap<String ,String>();
            map.put(Constant.COMMON_API_KEYS.AUTH_TOKEN.getKey() ,Constant.DEFAULT_AUTH_TOKEN);
            MyCustomRequest<SignUpActivityModel> request = new MyCustomRequest<SignUpActivityModel>(url , Request.Method.POST ,SignUpActivityModel.class ,map
                    ,new ResponseSuccessListener<SignUpActivityModel>(networkResponseListener ,Constant.SIGNUP_ACTIVITY_TAG) ,new ResponseErrorListener(networkResponseListener,Constant.SIGNUP_ACTIVITY_TAG));
            MySingleton.getInstance(SignUpOrganization.this).addToRequestQueue(request);
        }

    }

    /*
        * It call an api to get
        * country ,state ,region and town
        * list to show in
        * respected spinner or autocompleteview.
        * */
    private void makecallToSignupCountryListApi()
    {

        if(AppUtility.isNetworkAvailable(SignUpOrganization.this))
        {

            String url = Constant.BASE_URL+"getCountryList";
            Map<String ,String> map = new HashMap<String ,String>();
            map.put(Constant.COMMON_API_KEYS.AUTH_TOKEN.getKey() ,Constant.DEFAULT_AUTH_TOKEN);
            MyCustomRequest<SignUpCountryListModel> request = new MyCustomRequest<SignUpCountryListModel>(url , Request.Method.POST ,SignUpCountryListModel.class ,map
                    ,new ResponseSuccessListener<SignUpCountryListModel>(networkResponseListener ,Constant.SIGNUP_COUNTRY_TAG) ,new ResponseErrorListener(networkResponseListener,Constant.SIGNUP_COUNTRY_TAG));
            MySingleton.getInstance(SignUpOrganization.this).addToRequestQueue(request);
        }

    }

    /*
    * This listener receives response of server.
    * */
    NetworkResponseListener networkResponseListener = new NetworkResponseListener() {

        //On successful response of server request
        //call will be received here.
        @Override
        public void onNetworkResponseSuccess(Object successResponse, String tag) {

            if(tag.equalsIgnoreCase(Constant.SIGNUP_ACTIVITY_TAG))
            {
                setDataOnActivitySpinner((SignUpActivityModel)successResponse);
            }
            else if(tag.equalsIgnoreCase(Constant.SIGNUP_COUNTRY_TAG))
            {
                setDataOnCountrySpinner((SignUpCountryListModel)successResponse);
            }
            else if(tag.equalsIgnoreCase(Constant.SIGNUP_ORG_TAG))
            {
                AppUtility.hideProgress();
                performOperationOnSugnup((UserModel)successResponse);
            }else if(tag.equalsIgnoreCase(Constant.TERMS_COND_TAG))
            {

                performOperationOnTermsAndCond((BaseModel) successResponse);
            }

        }

        @Override
        public void onNetworkResponseFailure(VolleyError volleyError, String tag) {
            //On unsuccessful response of server request with reason
            //call will be received here.

            Toast.makeText(SignUpOrganization.this ,getString(R.string.some_wrong) ,Toast.LENGTH_SHORT).show();
            AppUtility.hideProgress();

        }
    };


    /*
    * After getting terms and condition
     * text ,it is stored in shared preference
      *  */
    private void performOperationOnTermsAndCond(BaseModel baseModel)
    {

        if(baseModel != null)
        {
            if(baseModel.status.equalsIgnoreCase("200"))
            {
                SharedPreferences pref = getSharedPreferences(Constant.SHARED_PREF ,MODE_PRIVATE);
                pref.edit().putString(Constant.TERMS_TEXT, baseModel.htmlText).commit();

            }

        }
    }

    /*
    * After getting response on sign up
    * API user credential is stored for
    * future use and redirected user into
    * the app.*/
    private void performOperationOnSugnup(UserModel signupOrganizationModel)
    {
        if(signupOrganizationModel.status.equalsIgnoreCase("200"))
        {

            SharedPreferences pref = getSharedPreferences(Constant.SHARED_PREF ,MODE_PRIVATE);
            CustomEditText email = (CustomEditText)findViewById(R.id.signup_org_email);
            CustomEditText password = (CustomEditText)findViewById(R.id.signup_org_password);
            pref.edit().putBoolean(Constant.IS_LOGIN ,false).commit();
            signupOrganizationModel.email = email.getText().toString();
            AppUtility.storeUserInfo(SignUpOrganization.this ,signupOrganizationModel ,email.getText().toString()
                    ,password.getText().toString());

            AppUtility.storeUserModel(SignUpOrganization.this, signupOrganizationModel);
            startActivity(new Intent(SignUpOrganization.this, SearchOrganization.class));
            finish();
        }
        else if(signupOrganizationModel.status.equalsIgnoreCase("408"))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.error_tit), signupOrganizationModel.message);
        }

    }

    /*
    * After getting activity list from server
    * it is set to activity spinner.
    * */
    private void setDataOnActivitySpinner(SignUpActivityModel activityModel)
    {
        if(activityModel.status.equalsIgnoreCase("200"))
        {
            TextView activityLabel = (TextView)findViewById(R.id.activity_label);
            Spinner activity = (Spinner)findViewById(R.id.signup_activity);
            if(activityModel.records != null)
            {
                activityLabel.setVisibility(View.GONE);
                activity.setVisibility(View.VISIBLE);

                SignUpActivityModel demoActivity = new SignUpActivityModel();
                SignUpActivityModel.records dactivity = demoActivity.new records();
                dactivity.activityName = "Select Activity";
                activityModel.records.add(0 ,dactivity);


                activity.setAdapter(new SignupActivityAdapter(SignUpOrganization.this ,activityModel.records));
            }
        }
    }

    /*
    * After getting country list from server
    * it is set to activity spinner.
    * */
    private void setDataOnCountrySpinner(SignUpCountryListModel countryModel)
    {
         if(countryModel.status.equalsIgnoreCase("200"))
         {
             Spinner country = (Spinner)findViewById(R.id.signup_country);

             TextView countryLabel = (TextView)findViewById(R.id.country_label);



             if(countryModel.records != null)
             {

                 country.setVisibility(View.VISIBLE);
                 countryLabel.setVisibility(View.GONE);


                 SignUpCountryListModel countrysModel = new SignUpCountryListModel();

                 SignUpCountryListModel.records countries = countrysModel.new records();
                 countries.countryName = getString(R.string.select_country);
                 countryModel.records.add(0, countries);
                SignUpCountryListModel.records.countryState  countryState = countries.new countryState();
                 countryState.stateName = getString(R.string.select_state);
                 countries.countryState.add(0, countryState);

                 country.setAdapter(new SignUpCountryAdapter(SignUpOrganization.this, countryModel.records));


             }
       }
    }

    /*
    * It makes call to server for
    * fetching terms and condition
    * from server.
    * */
    private void makeTermsConditionCall()
    {
        if(AppUtility.isNetworkAvailable(SignUpOrganization.this))
        {
            String url = Constant.BASE_URL + Constant.GET_TERM_CONDITIONS;
            Map<String ,String> map = new HashMap<String ,String>();
            map.put(Constant.COMMON_API_KEYS.AUTH_TOKEN.getKey() ,Constant.DEFAULT_AUTH_TOKEN);
            MyCustomRequest<BaseModel> request = new MyCustomRequest<BaseModel>(url , Request.Method.POST ,BaseModel.class ,map
                    ,new ResponseSuccessListener<BaseModel>(networkResponseListener ,Constant.TERMS_COND_TAG) ,new ResponseErrorListener(networkResponseListener,Constant.TERMS_COND_TAG));
            MySingleton.getInstance(SignUpOrganization.this).addToRequestQueue(request);

        }

    }


    /*
    * It makes call to server for
    * user registration.
    * */
    private void makeSignUpOrganizationCall(String token)
  {

      if(AppUtility.isNetworkAvailable(SignUpOrganization.this))
      {

          String url = Constant.BASE_URL + "signUpOrganization";
          MyCustomRequest<UserModel> request = new MyCustomRequest<UserModel>(url , Request.Method.POST ,UserModel.class ,getParameter(token)
                  ,new ResponseSuccessListener<UserModel>(networkResponseListener ,Constant.SIGNUP_ORG_TAG) ,new ResponseErrorListener(networkResponseListener,Constant.SIGNUP_ORG_TAG));
          MySingleton.getInstance(SignUpOrganization.this).addToRequestQueue(request);

      }
      else
      {
          AppUtility.showToast(SignUpOrganization.this, getString(R.string.netw_tit), getString(R.string.network_connection));
      }

  }

    /*
    * It prepare map as key
    * value pair to send parameter
    * for sign up api
    * */
    private Map<String ,String> getParameter(String token)
    {
        String email = ((TextView)findViewById(R.id.signup_org_email)).getText().toString();
        String password = ((TextView)findViewById(R.id.signup_org_password)).getText().toString();
        String contactPerson = ((TextView)findViewById(R.id.contact_personName)).getText().toString();
        String organizationName = ((TextView)findViewById(R.id.signup_orgname)).getText().toString();
        AutoCompleteTextView town = (AutoCompleteTextView)findViewById(R.id.signup_org_town);
        String townText = "";
        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SignupTownAdapter townAdapter = (SignupTownAdapter)town.getAdapter();

        if(townAdapter != null)
        {
            String twnName =  town.getText().toString();


            List<SignUpCountryListModel.records.countryState.regions.town> townList = townAdapter.getTownList();
            boolean flag = false;
            int pos = 0;

            for(int index=0;index<townList.size();index++)
            {
                if(twnName.equalsIgnoreCase(townList.get(index).townName))
                {
                    flag=true;
                    pos = index;
                }
            }

            if(flag)
            {
                townText = townList.get(pos).townId;

            }
            else
            {
                townText = twnName;
            }
        }
        else
        {
            townText = town.getText().toString();
        }


        String firstName="" ,lastName="";

        Spinner country = (Spinner)findViewById(R.id.signup_country);
        Spinner state = (Spinner)findViewById(R.id.signup_state);
        Spinner region = (Spinner)findViewById(R.id.signup_region);
        Spinner activity = (Spinner)findViewById(R.id.signup_activity);
        String selectedActivity = ((SignupActivityAdapter)activity.getAdapter())
                .getItem(activity.getSelectedItemPosition()).activityId;
        String selectedCountry = ((SignUpCountryAdapter)country.getAdapter())
                .getItem(country.getSelectedItemPosition()).countryId;
        String selectedState =((SignUpStateAdapter)state.getAdapter())
                .getItem(state.getSelectedItemPosition()).stateId;
        String selectedRegion = ((SignUpRegionListAdapter)region.getAdapter())
                .getItem(region.getSelectedItemPosition()).regionId;




        if(contactPerson.contains(" "))
        {
            String s[] = contactPerson.split(" ");
            if(s.length>2)
            {
                firstName=s[0];
                for(int index=0;index<s.length;index++)
                {
                    if(index!=0)
                    {
                        lastName = lastName+" "+s[index];
                    }
                }
            }
            else
            {
                firstName=s[0];
                lastName=s[1];
            }
        }
        else
        {
            firstName = contactPerson;
        }

        Map<String ,String> map = new HashMap<String ,String>();
        map.put(Constant.COMMON_API_KEYS.EMAIL.getKey() ,email);
        map.put(Constant.COMMON_API_KEYS.PASSWORD.getKey() ,password);
        map.put(Constant.COMMON_API_KEYS.DEVICETYPE.getKey() ,"A");
        map.put(Constant.COMMON_API_KEYS.DEVICETOKEN.getKey() ,token);
        map.put(Constant.COMMON_API_KEYS.STATE_ID.getKey() ,selectedState);
        map.put(Constant.COMMON_API_KEYS.COUNTRY_ID.getKey() ,selectedCountry);
        map.put(Constant.COMMON_API_KEYS.REGION_ID.getKey() ,selectedRegion);
        map.put(Constant.COMMON_API_KEYS.ACTIVITY_ID.getKey() ,selectedActivity);
        map.put(Constant.COMMON_API_KEYS.ORGANIZATION_NAME.getKey() ,organizationName);
        map.put(Constant.COMMON_API_KEYS.FIRST_NAME.getKey() ,firstName.trim());
        map.put(Constant.COMMON_API_KEYS.LAST_NAME.getKey() ,lastName.trim());
        map.put(Constant.COMMON_API_KEYS.TOWN.getKey() ,townText);
        map.put(Constant.COMMON_API_KEYS.DEVICE_ID.getKey() ,androidId);

        SharedPreferences pref = getSharedPreferences(Constant.SHARED_PREF ,MODE_PRIVATE);
        pref.edit().putString(Constant.PF_DEVICE_TOKEN ,androidId).commit();

        return map;

    }

    /*
    * It makes underline text text of
    * terms and condtion sentence clickable.
    * */
    private void makeTermsConditionsClickcable()
    {
        String str = getString(R.string.signup_term_con);
        int i1 = str.indexOf("Te");
        int i2 = str.indexOf("se");

        CustomTextView termsTextview = (CustomTextView)findViewById(R.id.terms_condition);
        termsTextview.setText(str);
        termsTextview.setMovementMethod(LinkMovementMethod.getInstance());
        Spannable mySpannable = (Spannable)termsTextview.getText();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view)
            {
                addTermsFragment();
            }
        };
        mySpannable.setSpan(clickableSpan, i1, i2 + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    }


    /*
    * It adds terms and condition
    * fragment when it is called*/
    private void addTermsFragment()
    {
        SharedPreferences pref = getSharedPreferences(Constant.SHARED_PREF, MODE_PRIVATE);
        String text =  pref.getString(Constant.TERMS_TEXT, null);
        if(text != null)
        {
            TermsAndConditionsFragment termsAndConditionsFragment = new TermsAndConditionsFragment();
            Bundle bundle = new Bundle();
            bundle.putString(TermsAndConditionsFragment.class.getSimpleName(), text);
            termsAndConditionsFragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout ,termsAndConditionsFragment ,Constant.TERM_FRAGMENT_TAG).commit();
        }
    }

     /*
      *This method validates data
      * enter by user before
      *sending to server.
      * */
    private boolean validateData()
    {
        String organizationName = ((CustomEditText)findViewById(R.id.signup_orgname)).getText().toString().trim();
        String email = ((CustomEditText)findViewById(R.id.signup_org_email)).getText().toString().trim();
        String password = ((CustomEditText)findViewById(R.id.signup_org_password)).getText().toString().trim();

        String town = ((AutoCompleteTextView)findViewById(R.id.signup_org_town)).getText().toString().trim();
        String contactPersonName = ((CustomEditText)findViewById(R.id.contact_personName)).getText().toString().trim();
        Spinner country = (Spinner)findViewById(R.id.signup_country);
        Spinner state = (Spinner)findViewById(R.id.signup_state);
        Spinner region = (Spinner)findViewById(R.id.signup_region);
        Spinner activity = (Spinner)findViewById(R.id.signup_activity);
        String selectedActivity = "";
        String selectedCountry = "";
        String selectedState = "";
        String selectedRegion = "";

        CheckBox termCondition = (CheckBox)findViewById(R.id.term_con);

        if(activity.getAdapter()!=null)
        {
            if(activity.getSelectedItemPosition()!=0)
            {
                selectedActivity = ((SignupActivityAdapter)activity.getAdapter())
                        .getItem(activity.getSelectedItemPosition()).activityId;
            }

        }

        if(country.getAdapter()!=null)
        {
            if(country.getSelectedItemPosition()!=0)
            {
                selectedCountry = ((SignUpCountryAdapter)country.getAdapter())
                        .getItem(country.getSelectedItemPosition()).countryId;
            }

        }

        if(state.getAdapter()!=null)
        {
            if(state.getSelectedItemPosition()!=0)
            {
                selectedState = ((SignUpStateAdapter)state.getAdapter())
                        .getItem(state.getSelectedItemPosition()).stateId;
            }

        }

        if(region.getAdapter()!=null)
        {
            if(region.getSelectedItemPosition()!=0)
            {
                selectedRegion = ((SignUpRegionListAdapter)region.getAdapter())
                        .getItem(region.getSelectedItemPosition()).regionId;
            }

        }


        if(organizationName.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_org_emp));
            return false;
        }
        else if(organizationName.length()<6)
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_org_min_inv));
            return false;
        }
        else if(!organizationName.matches(Constant.ZIPCODE_EXP))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_org_inv));
            return false;
        }
        else if(selectedActivity.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_activity_valid));
            return false;
        }
        else if(contactPersonName.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_con_pr_empty));
            return false;
        }
        else if(!contactPersonName.matches(Constant.APHA_EXP))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_con_pr_inv));
            return false;
        }
        else if(email.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.empty_usrname));
            return false;
        }
        else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.invalid_usrname));
            return false;
        }
        else if(password.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.empty_pswd));
            return false;
        }
        else if(password.length()<4)
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.invalid_password));
            return false;
        }
        else if(selectedCountry.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_countryy_valid));
            return false;
        }
        else if(selectedState.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_state_valid));
            return false;
        }
        else if(selectedRegion.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_region_valid));
            return false;
        }
        else if(town.equalsIgnoreCase(""))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_town));
            return false;
        }
        else if(!town.matches(Constant.APHA_EXP))
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_town_inv));
            return false;
        }
        else if(!termCondition.isChecked())
        {
            AppUtility.showToast(SignUpOrganization.this, getString(R.string.valid_tit), getString(R.string.sign_up_mem_term_co));
            return false;
        }
        else
        {
            return true;
        }
    }


    /*
    * It makes underline Sign In text
    * clickable.
    * */
    private void makeUnderlineTextClickcable()
    {
        String str = getString(R.string.login_alrdy_member_txt);
        int i1 = str.indexOf("Si");
        int i2 = str.indexOf("In");

        TextView textview = (TextView)findViewById(R.id.signin_redirection);
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
                startActivity(new Intent(SignUpOrganization.this ,Login.class));
                finish();
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
   * and device token will be receive from GCM server
   * */
    private class GetToken extends AsyncTask<String ,String ,String> {

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
                SharedPreferences pref = context.getSharedPreferences(Constant.SHARED_PREF, MODE_PRIVATE);
                pref.edit().putString(Constant.PF_DEVICE_TOKEN ,token).commit();

            } catch (Exception e) {

                e.printStackTrace();
            }


            return token;
        }


        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);

            if(token!=null && !token.equalsIgnoreCase(""))
            {

                makeSignUpOrganizationCall(token);
            }
            else
            {
                AppUtility.hideProgress();
                AppUtility.showToast(SignUpOrganization.this ,"",getString(R.string.token_unavail));
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
