package __PACKAGE_NAME__.infrastructure.sample;

import __PACKAGE_NAME__.domain.gateway.SampleGateway;
import __PACKAGE_NAME__.domain.model.SampleEntity;
import __PACKAGE_NAME__.infrastructure.sample.entity.Sample;
import __PACKAGE_NAME__.infrastructure.sample.mapper.SampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SampleGatewayImpl implements SampleGateway {

    private final SampleMapper sampleMapper;

    @Override
    public List<SampleEntity> listAll() {
        return sampleMapper.selectList(null).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SampleEntity getById(Long id) {
        Sample sample = sampleMapper.selectById(id);
        return sample == null ? null : toEntity(sample);
    }

    @Override
    public int save(SampleEntity entity) {
        Sample sample = toDO(entity);
        int rows = sampleMapper.insert(sample);
        entity.setId(sample.getId());
        return rows;
    }

    private SampleEntity toEntity(Sample dataObject) {
        SampleEntity entity = new SampleEntity();
        BeanUtils.copyProperties(dataObject, entity);
        return entity;
    }

    private Sample toDO(SampleEntity entity) {
        Sample dataObject = new Sample();
        BeanUtils.copyProperties(entity, dataObject);
        return dataObject;
    }
}
