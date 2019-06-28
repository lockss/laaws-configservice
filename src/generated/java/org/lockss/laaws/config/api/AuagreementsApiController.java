package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class AuagreementsApiController implements AuagreementsApi {

    private final AuagreementsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public AuagreementsApiController(AuagreementsApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public AuagreementsApiDelegate getDelegate() {
        return delegate;
    }
}
