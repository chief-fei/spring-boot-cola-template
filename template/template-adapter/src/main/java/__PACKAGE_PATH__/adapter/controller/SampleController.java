package __PACKAGE_NAME__.adapter.controller;

import __PACKAGE_NAME__.app.service.SampleAppService;
import __PACKAGE_NAME__.client.annotation.Log;
import __PACKAGE_NAME__.client.dto.Result;
import __PACKAGE_NAME__.domain.model.SampleEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sample", description = "Sample API")
@RestController
@RequestMapping("/api/sample")
@RequiredArgsConstructor
public class SampleController {

    private final SampleAppService sampleAppService;

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("ok");
    }

    @Operation(summary = "Get sample by id")
    @Log(module = "Sample", operation = "GetById")
    @GetMapping("/{id}")
    public Result<SampleEntity> getById(@PathVariable Long id) {
        return Result.ok(sampleAppService.getById(id));
    }

    @Operation(summary = "List all samples")
    @Log(module = "Sample", operation = "ListAll")
    @GetMapping("/list")
    public Result<List<SampleEntity>> list() {
        return Result.ok(sampleAppService.listAll());
    }
}
