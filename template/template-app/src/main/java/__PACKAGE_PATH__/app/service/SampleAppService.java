package __PACKAGE_NAME__.app.service;

import __PACKAGE_NAME__.domain.gateway.SampleGateway;
import __PACKAGE_NAME__.domain.model.SampleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SampleAppService {

    private final SampleGateway sampleGateway;

    public SampleEntity getById(Long id) {
        return sampleGateway.getById(id);
    }

    public List<SampleEntity> listAll() {
        return sampleGateway.listAll();
    }
}
