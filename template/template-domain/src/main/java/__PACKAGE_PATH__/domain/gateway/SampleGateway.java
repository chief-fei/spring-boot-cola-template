package __PACKAGE_NAME__.domain.gateway;

import __PACKAGE_NAME__.domain.model.SampleEntity;

import java.util.List;

public interface SampleGateway {

    List<SampleEntity> listAll();

    SampleEntity getById(Long id);

    int save(SampleEntity entity);
}
