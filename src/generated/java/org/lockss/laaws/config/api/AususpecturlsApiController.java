package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class AususpecturlsApiController implements AususpecturlsApi {

    private final AususpecturlsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public AususpecturlsApiController(AususpecturlsApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public AususpecturlsApiDelegate getDelegate() {
        return delegate;
    }
}
