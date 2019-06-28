package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class NoaupeersApiController implements NoaupeersApi {

    private final NoaupeersApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public NoaupeersApiController(NoaupeersApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public NoaupeersApiDelegate getDelegate() {
        return delegate;
    }
}
