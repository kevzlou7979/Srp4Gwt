/*
 * #%L
 * GwtMaterial
 * %%
 * Copyright (C) 2015 - 2017 GwtMaterialDesign
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.legioth.srp4gwt.demo.client.application;

import com.github.legioth.srp4gwt.client.Srp6Client;
import com.github.legioth.srp4gwt.demo.shared.GreetingService;
import com.github.legioth.srp4gwt.demo.shared.GreetingServiceAsync;
import com.github.legioth.srp4gwt.shared.*;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.googlecode.gwt.crypto.bouncycastle.CryptoException;
import com.gwtplatform.mvp.client.ViewImpl;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.*;

import java.math.BigInteger;

public class ApplicationView extends ViewImpl implements ApplicationPresenter.MyView {
    interface Binder extends UiBinder<Widget, ApplicationView> {
    }

    private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);
    private final Srp6Configuration configuration = new DefaultSrp6Configuration();


    @UiField
    MaterialTextBox usernameField, passwordField;

    @UiField
    MaterialRow loggingArea;

    @UiField
    MaterialButton login, register;

    @UiField
    MaterialContainer container;

    @Inject
    ApplicationView(Binder uiBinder) {
        initWidget(uiBinder.createAndBindUi(this));
    }


    @Override
    protected void onAttach() {
        super.onAttach();

        log("Using " + configuration.N().bitLength() + " bit prime: " + configuration.N(), LogStatus.INFO);
    }

    @UiHandler("register")
    void sendVerifier(ClickEvent e) {
        // Initialize SRP handler
        Srp6Client verifierGenerator = new Srp6Client(configuration);

        byte[] salt = new byte[configuration.digest().getDigestSize()];
        configuration.random().nextBytes(salt);

        String username = usernameField.getText();
        String password = passwordField.getText();

        Duration duration = new Duration();
        BigInteger verifier = verifierGenerator.generateVerifier(salt, username, password);
        log("Verifier generated in " + duration.elapsedMillis() + " ms", LogStatus.INFO);

        log("Sending verifier to server: " + verifier, LogStatus.INFO);

        greetingService.submitVerifier(username, salt, verifier,
                new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        log("Error sending verifier: " + caught.getMessage(), LogStatus.INFO);
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(Void result) {
                        log("Verifier sent to the server", LogStatus.SUCCESS);
                    }
                });

    }

    @UiHandler("login")
    void authenticate(ClickEvent event) {
        final Srp6Client client = new Srp6Client(configuration);

        log("Generating initialization request", LogStatus.INFO);

        Srp6InitRequest initRequest = client.generateInitRequest(usernameField.getText());

        log("Sending initialization request with A = " + initRequest.getA(), LogStatus.INFO);

        greetingService.init(initRequest,
                new AsyncCallback<Srp6InitResponse>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        log("Auhtentication initialization failed", LogStatus.ERROR);
                    }

                    @Override
                    public void onSuccess(Srp6InitResponse initResponse) {
                        log("Got init response with B = " + initResponse.getB(), LogStatus.SUCCESS);
                        sendProof(client, initResponse);
                    }
                });

    }

    private void sendProof(final Srp6Client client, Srp6InitResponse result) {
        String password = this.passwordField.getValue();
        try {
            Srp6VerificationRequest verificationRequest = client.generateVerificationRequest(result, password);

            log("Sending verification request with M1 = " + verificationRequest.getM1(), LogStatus.INFO);

            greetingService.verify(verificationRequest,
                    new AsyncCallback<Srp6VerificationResponse>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            log("Error validating m1. " + caught.getMessage(), LogStatus.ERROR);
                            caught.printStackTrace();
                        }

                        @Override
                        public void onSuccess(Srp6VerificationResponse response) {
                            if (response == null) {
                                log("Server did not accept the M1 value", LogStatus.ERROR);
                            } else {
                                log("Got verification response with M2 = " + response.getM2(), LogStatus.SUCCESS);
                                BigInteger sessionKey = client.getSessionKey(response);
                                if (sessionKey != null) {
                                    log("Authentication completed with session key " + sessionKey, LogStatus.INFO);
                                } else {
                                    log("Final authentication verification failed", LogStatus.ERROR);
                                }
                            }
                        }
                    });
        } catch (CryptoException e) {
            log("Could not calculate secret. " + e.getLocalizedMessage(), LogStatus.ERROR);
            e.printStackTrace();
        }
    }

    private void log(String message, LogStatus status) {
        message = Duration.currentTimeMillis() + ": " + message;

        MaterialLink link = new MaterialLink();
        link.setGrid("s12");
        link.setText(message);
        link.setDisplay(Display.INLINE_BLOCK);

        if (status == LogStatus.INFO) {
            link.setIconType(IconType.INFO);
        } else if (status == LogStatus.ERROR) {
            link.setTextColor(Color.RED);
            link.setIconType(IconType.ERROR);
        } else if (status == LogStatus.SUCCESS) {
            link.setTextColor(Color.GREEN);
            link.setIconType(IconType.CHECK_CIRCLE);
        }


        loggingArea.add(link);

        // Scroll to the end
        loggingArea.getElement().setScrollTop(Integer.MAX_VALUE);
    }
}
