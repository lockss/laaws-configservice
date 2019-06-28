package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class AustatesApiController implements AustatesApi {

    private final AustatesApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public AustatesApiController(AustatesApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public AustatesApiDelegate getDelegate() {
        return delegate;
    }
}
