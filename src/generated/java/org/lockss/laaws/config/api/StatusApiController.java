package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class StatusApiController implements StatusApi {

    private final StatusApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public StatusApiController(StatusApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public StatusApiDelegate getDelegate() {
        return delegate;
    }
}
