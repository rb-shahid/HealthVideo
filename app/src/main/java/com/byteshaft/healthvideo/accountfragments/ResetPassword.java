package com.byteshaft.healthvideo.accountfragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;


public class ResetPassword extends Fragment implements View.OnClickListener, HttpRequest.OnReadyStateChangeListener, HttpRequest.OnErrorListener {

    private View mBaseView;
    private EditText mEmail;
    private EditText mOldPassword;
    private EditText mNewPassword;
    private Button mResetButton;
    private String mEmailString;
    private String mOldPasswordString;
    private String mNewPasswordString;
    private HttpRequest request;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.fragment_reset_password, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(getResources().getString(R.string.reset_password));
        setHasOptionsMenu(true);
        mEmail = (EditText) mBaseView.findViewById(R.id.email_edit_text);
        mOldPassword = (EditText) mBaseView.findViewById(R.id.old_password_edit_text);
        mNewPassword = (EditText) mBaseView.findViewById(R.id.new_password_edit_text);
        mResetButton = (Button) mBaseView.findViewById(R.id.button_reset);

        mEmail.setText(AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_EMAIL));

        mResetButton.setOnClickListener(this);
        return mBaseView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                return true;
            default:return false;
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_reset:
                if (validateEditText()) {
                    changePassword(mEmailString, mOldPasswordString, mNewPasswordString);
                }
                break;
        }
    }

    private boolean validateEditText() {
        boolean valid = true;
        mNewPasswordString = mNewPassword.getText().toString();
        mOldPasswordString = mOldPassword.getText().toString();
        mEmailString = mEmail.getText().toString();


        if (mNewPasswordString.trim().isEmpty() || mNewPasswordString.length() < 4) {
            mNewPassword.setError("enter at least 3 characters");
            valid = false;
        } else {
            mNewPassword.setError(null);
        }

        if (mOldPasswordString.trim().isEmpty() || mOldPasswordString.length() < 4) {
            mOldPassword.setError("enter at least 3 characters");
            valid = false;
        } else {
            mOldPassword.setError(null);
        }

        if (mEmailString.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mEmailString).matches()) {
            mEmail.setError("please provide a valid email");
            valid = false;
        } else {
            mEmail.setError(null);
        }
        return valid;
    }

    private void changePassword(String email, String emailotp, String newpassword) {
        request = new HttpRequest(getActivity());
        request.setOnReadyStateChangeListener(this);
        request.setOnErrorListener(this);
        request.open("POST", String.format("%schange-password", AppGlobals.BASE_URL));
        request.send(getUserChangePassword(email, emailotp, newpassword));
    }


    private String getUserChangePassword(String email, String emailotp, String newpassword) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("email", email);
            jsonObject.put("email_otp", emailotp);
            jsonObject.put("new_password", newpassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();

    }

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {

        switch (readyState) {
            case HttpRequest.STATE_DONE:
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println(request.getResponseText() + "working ");
                }
        }

    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.replace(R.id.container, fragment);
        tx.commit();
    }

    @Override
    public void onError(HttpRequest request, int readyState, short error, Exception exception) {
        if (exception.getLocalizedMessage().equals("Network is unreachable")) {
            Helpers.showSnackBar(getView(), exception.getLocalizedMessage());
        }
        switch (readyState) {
            case HttpRequest.ERROR_CONNECTION_TIMED_OUT:
                Helpers.showSnackBar(getView(), "connection time out");
                break;
        }

    }
}
