package com.github.legioth.srp4gwt.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface Srp4ClientBundle extends ClientBundle {

    Srp4ClientBundle INSTANCE = GWT.create(Srp4ClientBundle.class);

    @Source("js/jsbn.js")
    TextResource jsBinaryLibrary();
}
