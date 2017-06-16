package com.byteshaft.healthvideo.accountfragments;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.FormData;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

public class SignUp extends Fragment implements View.OnClickListener,
        HttpRequest.OnReadyStateChangeListener, HttpRequest.OnErrorListener {

    private View mBaseView;
    private EditText mEmail;
    private EditText mPassword;
    private EditText mVerifyPassword;
    private Button mSignUpButton;
    private String mEmailAddressString;
    private String mPasswordString;
    private String mVerifyPasswordString;
    private String mCheckBoxString = "patient";
    private HttpRequest request;
    private EditText firstName;
    private EditText lastName;
    private String deviceId;
    private TelephonyManager telephonyManager;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.fragment_sign_up, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(getResources().getString(R.string.sign_up));
        telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        setHasOptionsMenu(true);
        firstName = (EditText) mBaseView.findViewById(R.id.first_name);
        lastName = (EditText) mBaseView.findViewById(R.id.last_name);
        mEmail = (EditText) mBaseView.findViewById(R.id.email_edit_text);
        mPassword = (EditText) mBaseView.findViewById(R.id.password_edit_text);
        mVerifyPassword = (EditText) mBaseView.findViewById(R.id.verify_password_edit_text);
        mSignUpButton = (Button) mBaseView.findViewById(R.id.sign_up_button);
        askPermission();

        mSignUpButton.setOnClickListener(this);
        return mBaseView;
    }

    private void askPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        } else {
            deviceId = telephonyManager.getDeviceId();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Snackbar.make(getView(), getResources().getString(R.string.permission_granted),
                            Snackbar.LENGTH_SHORT).show();
                    deviceId = telephonyManager.getDeviceId();

                } else {
                    Snackbar.make(getView(), getResources().getString(R.string.permission_denied),
                            Snackbar.LENGTH_SHORT).show();

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_up_button:
                if (validateEditText()) {
                    registerUser(firstName.getText().toString(), lastName.getText().toString(),
                            mPasswordString, mEmailAddressString);
                }
                break;
        }
    }


    private boolean validateEditText() {
        boolean valid = true;
        mEmailAddressString = mEmail.getText().toString();
        mPasswordString = mPassword.getText().toString();
        mVerifyPasswordString = mVerifyPassword.getText().toString();

        if (deviceId == null || deviceId.trim().isEmpty()) {
            valid = false;
            Snackbar.make(getView(), getResources().getString(R.string.grant_permission),
                    Snackbar.LENGTH_SHORT).show();
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    askPermission();
                }
            }, 2000);
        }

        if (firstName.getText().toString().trim().isEmpty() || firstName.getText().toString().length() < 1) {
            firstName.setError("enter a valid name");
            valid = false;
        }

        if (lastName.getText().toString().trim().isEmpty() || lastName.getText().toString().length() < 1) {
            lastName.setError("enter a valid name");
            valid = false;
        }


        if (mEmailAddressString.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mEmailAddressString).matches()) {
            mEmail.setError("please provide a valid email");
            valid = false;
        } else {
            mEmail.setError(null);
        }
        if (mPasswordString.trim().isEmpty() || mPasswordString.length() < 6) {
            mPassword.setError("enter at least 6 character password");
            valid = false;
        } else {
            mPassword.setError(null);
        }

        if (mVerifyPasswordString.trim().isEmpty() || mVerifyPasswordString.length() < 6 ||
                !mVerifyPasswordString.equals(mPasswordString)) {
            mVerifyPassword.setError("password does not match");
            valid = false;
        } else {
            mVerifyPassword.setError(null);
        }
        return valid;
    }

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                Helpers.dismissProgressDialog();
                Log.i("TAG", "Response " + request.getResponseText());
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println(request.getResponseText());
                        try {
                            JSONObject jsonObject = new JSONObject(request.getResponseText());
                            if (jsonObject.getBoolean("success")) {
                                Helpers.showSnackBar(getView(),
                                        getResources().getString(R.string.created_account_successfully));
                                AppGlobals.saveDataToSharedPreferences(AppGlobals.KEY_USER_NAME,
                                        jsonObject.getString("result"));
                                AlertDialog alertDialog;
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                                alertDialogBuilder.setTitle(getResources()
                                        .getString(R.string.created_account_successfully));
                                alertDialogBuilder.setMessage(Html.fromHtml("Please visit your email to activate your account and login.")).setCancelable(false)
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                                getFragmentManager().popBackStack();

                                            }
                                        });

                                alertDialog = alertDialogBuilder.create();
                                alertDialog.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                }
        }

    }

    @Override
    public void onError(HttpRequest request, int readyState, short error, Exception exception) {
        Helpers.dismissProgressDialog();
        switch (readyState) {
            case HttpRequest.ERROR_CONNECTION_TIMED_OUT:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.connection_time_out));
                break;
            case HttpRequest.ERROR_NETWORK_UNREACHABLE:
                Helpers.showSnackBar(getView(), exception.getLocalizedMessage());
                break;
        }

    }

    private void registerUser(String firstName, String lastName, String password, String email) {
        FormData formData = new FormData();
        formData.append(FormData.TYPE_CONTENT_TEXT, "firstname", firstName);
        formData.append(FormData.TYPE_CONTENT_TEXT, "lastname", lastName);
        formData.append(FormData.TYPE_CONTENT_TEXT, "email", email);
        formData.append(FormData.TYPE_CONTENT_TEXT, "password", password);
        formData.append(FormData.TYPE_CONTENT_TEXT, "deviceid", deviceId);
        formData.append(FormData.TYPE_CONTENT_TEXT, "language", AppGlobals.sLanguage);
        request = new HttpRequest(getActivity());
        request.setOnReadyStateChangeListener(this);
        request.setOnErrorListener(this);
        request.open("POST", String.format("%ssignup", AppGlobals.BASE_URL));
        request.send(formData);
        Helpers.showProgressDialog(getActivity(), "processing...");
    }
}
