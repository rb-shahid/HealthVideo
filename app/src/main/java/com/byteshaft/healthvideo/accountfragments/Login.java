package com.byteshaft.healthvideo.accountfragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.MainActivity;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.FormData;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

public class Login extends Fragment implements View.OnClickListener, HttpRequest.OnErrorListener,
        HttpRequest.OnReadyStateChangeListener {

    private View mBaseView;
    private EditText mEmail;
    private EditText mPassword;
    private Button mLoginButton;
    private TextView mSignUpTextView;
    private String mPasswordString;
    private String mEmailString;
    private HttpRequest request;
    private TextView forgotPasswordTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.fragment_login, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(getResources().getString(R.string.login));
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mEmail = (EditText) mBaseView.findViewById(R.id.email_edit_text);
        mPassword = (EditText) mBaseView.findViewById(R.id.password_edit_text);
        mLoginButton = (Button) mBaseView.findViewById(R.id.button_login);
        mSignUpTextView = (TextView) mBaseView.findViewById(R.id.sign_up_text_view);
        forgotPasswordTextView = (TextView) mBaseView.findViewById(R.id.forgot_password);
        mLoginButton.setOnClickListener(this);
        forgotPasswordTextView.setOnClickListener(this);
        mSignUpTextView.setOnClickListener(this);
        return mBaseView;
    }

    @Override
    public void onPause() {
        super.onPause();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        mEmailString = mEmail.getText().toString();
        mPasswordString = mPassword.getText().toString();

        if (mEmailString.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mEmailString).matches()) {
            mEmail.setError("please provide a valid email");
            valid = false;
        } else {
            mEmail.setError(null);
        }
        if (mPasswordString.isEmpty() || mPassword.length() < 6) {
            mPassword.setError("password must be of 6 characters");
            valid = false;
        } else {
            mPassword.setError(null);
        }
        return valid;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                AccountManagerActivity.getInstance().finish();
                return true;
            default: return false;
        }
    }

    private void loginUser(String email, String password) {
        Helpers.showProgressDialog(getActivity(), "processing...");
        FormData formData = new FormData();
        formData.append(FormData.TYPE_CONTENT_TEXT, "email", email);
        formData.append(FormData.TYPE_CONTENT_TEXT, "password", password);
        request = new HttpRequest(getActivity());
        request.setOnReadyStateChangeListener(this);
        request.setOnErrorListener(this);
        request.open("POST", String.format("%slogin", AppGlobals.BASE_URL));
        request.send(formData);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_login:
                if (validate()) {
                    loginUser(mEmailString, mPasswordString);
                }
                break;
            case R.id.forgot_password:
                AccountManagerActivity.getInstance().loadFragment(new ForgotPassword());
                break;
            case R.id.sign_up_text_view:
                AccountManagerActivity.getInstance().loadFragment(new SignUp());
                break;

        }
    }

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                Helpers.dismissProgressDialog();
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println(request.getResponseText());
                        try {
                            JSONObject jsonObject = new JSONObject(request.getResponseText());
                            if (jsonObject.isNull("error") && jsonObject.getBoolean("success")) {
                                JSONObject result = jsonObject.getJSONObject("result");
                                AppGlobals.saveDataToSharedPreferences(AppGlobals.KEY_TOKEN,
                                        result.getString("token"));
                                AppGlobals.saveDataToSharedPreferences(AppGlobals.KEY_USER_NAME,
                                        result.getString("username"));
                                AppGlobals.loginState(true);
                                startActivity(new Intent(getActivity(), MainActivity.class));
                            } else if (!jsonObject.isNull("error") && !jsonObject.getBoolean("success")) {
                                if (jsonObject.getString("error").equals("BAD_CREDENTIALS")) {
                                    Snackbar.make(getView(), "Please check login credentials", Snackbar.LENGTH_LONG).show();
                                } else if (jsonObject.getString("error").equals("USER_INACTIVE")) {
                                    Helpers.alertDialog(R.drawable.ic_alert, getActivity(), getResources().getString(R.string.user_inactive),
                                            getResources().getString(R.string.user_inactive_message));
                                } else if (jsonObject.getString("error").equals("USER_NOT_FOUND")) {
                                    Helpers.alertDialog(R.drawable.not_found, getActivity(), getResources().getString(R.string.not_found),
                                            getResources().getString(R.string.not_found_message));
                                }
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
            case HttpRequest.ERROR_SSL_CERTIFICATE_INVALID:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.hand_shake_error));


        }

    }

}
