package com.github.legioth.srp4gwt.demo.shared;

import com.github.legioth.srp4gwt.shared.Srp6InitRequest;
import com.github.legioth.srp4gwt.shared.Srp6InitResponse;
import com.github.legioth.srp4gwt.shared.Srp6VerificationRequest;
import com.github.legioth.srp4gwt.shared.Srp6VerificationResponse;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.math.BigInteger;

public interface GreetingServiceAsync {
    void submitVerifier(String username, byte[] salt, BigInteger verifier,
                        AsyncCallback<Void> callback);

    void init(Srp6InitRequest initRequest,
              AsyncCallback<Srp6InitResponse> callback);

    void verify(Srp6VerificationRequest verificationRequest,
                AsyncCallback<Srp6VerificationResponse> callback);
}