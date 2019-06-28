package org.lockss.laaws.mdq.api;

import org.springframework.stereotype.Controller;

@Controller
public class UrlsApiController implements UrlsApi {

    private final UrlsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public UrlsApiController(UrlsApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public UrlsApiDelegate getDelegate() {
        return delegate;
    }
}
