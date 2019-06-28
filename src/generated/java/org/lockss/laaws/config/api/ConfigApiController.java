package org.lockss.laaws.config.api;

import org.springframework.stereotype.Controller;

@Controller
public class ConfigApiController implements ConfigApi {

    private final ConfigApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public ConfigApiController(ConfigApiDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public ConfigApiDelegate getDelegate() {
        return delegate;
    }
}
