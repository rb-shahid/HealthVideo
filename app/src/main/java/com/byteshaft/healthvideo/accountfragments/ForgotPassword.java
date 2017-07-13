package com.byteshaft.healthvideo.accountfragments;

import android.os.Bundle;
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
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.HttpRequest;

import java.net.HttpURLConnection;

public class ForgotPassword extends Fragment implements View.OnClickListener, HttpRequest.OnErrorListener,
        HttpRequest.OnReadyStateChangeListener {

    private View mBaseView;
    private EditText mEmail;
    private Button mRecoverButton;
    private String mEmailString;
    private HttpRequest request;
    private TextView forgetPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.fragment_forgot_password, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(getResources().getString(R.string.forgot_password));
        setHasOptionsMenu(true);
        mEmail = (EditText) mBaseView.findViewById(R.id.email_edit_text);
        mRecoverButton = (Button) mBaseView.findViewById(R.id.button_recover);
        forgetPassword = (TextView) mBaseView.findViewById(R.id.text_change_password);
        mRecoverButton.setOnClickListener(this);
        mEmail.setTypeface(AppGlobals.normalTypeFace);
        mRecoverButton.setTypeface(AppGlobals.normalTypeFace);
        forgetPassword.setTypeface(AppGlobals.normalTypeFace);
        mEmail.setText(AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_EMAIL));
        mEmailString = AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_EMAIL);
        return mBaseView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                return true;
            default:return false;
        }
    }


    public boolean validate() {
        boolean valid = true;
        mEmailString = mEmail.getText().toString();

        if (mEmailString.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mEmailString).matches()) {
            mEmail.setError("please provide a valid email");
            valid = false;
        } else {
            mEmail.setError(null);
        }
        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_recover:
                if (validate()) {
                    recoverUserPassword(mEmailString);
                }
                break;
        }
    }


    private void recoverUserPassword(String email) {
        request = new HttpRequest(getActivity());
        request.setOnReadyStateChangeListener(this);
        request.setOnErrorListener(this);
        request.open("GET", String.format("%sforgotpassword?email="+email, AppGlobals.BASE_URL));
        request.send();
        Helpers.showProgressDialog(getActivity(), "Processing...");
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

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                Helpers.dismissProgressDialog();
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        System.out.println(request.getResponseText());
                        break;
                }
        }

    }

}
