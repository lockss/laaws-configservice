package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class AusApiController implements AusApi {

    private final AusApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public AusApiController(AusApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public AusApiDelegate getDelegate() {
        return delegate;
    }
}
