package com.devtwin.api;

import com.devtwin.twinengine.DeveloperTwin;
import com.devtwin.twinengine.TwinPipelineService;
import com.devtwin.twinengine.TwinService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/twin")
public class TwinController {

    private static final String GITHUB_USERNAME_PATTERN = "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$";

    private final TwinService twinService;
    private final TwinPipelineService twinPipelineService;

    public TwinController(TwinService twinService, TwinPipelineService twinPipelineService) {
        this.twinService = twinService;
        this.twinPipelineService = twinPipelineService;
    }

    @GetMapping("/{username}")
    public DeveloperTwin get(
            @PathVariable
            @Pattern(regexp = GITHUB_USERNAME_PATTERN, message = "Invalid GitHub username")
            String username
    ) {
        return twinService.get(username);
    }

    @PostMapping("/{username}/build")
    public DeveloperTwin build(
            @PathVariable
            @Pattern(regexp = GITHUB_USERNAME_PATTERN, message = "Invalid GitHub username")
            String username
    ) {
        return twinPipelineService.build(username);
    }
}
