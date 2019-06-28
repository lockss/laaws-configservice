package org.lockss.laaws.mdq.api;

import org.springframework.stereotype.Controller;

@Controller
public class MetadataApiController implements MetadataApi {

    private final MetadataApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public MetadataApiController(MetadataApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public MetadataApiDelegate getDelegate() {
        return delegate;
    }
}
