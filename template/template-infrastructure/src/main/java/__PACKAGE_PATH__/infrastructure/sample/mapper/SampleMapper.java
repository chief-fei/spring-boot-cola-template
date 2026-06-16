package __PACKAGE_NAME__.infrastructure.sample.mapper;

import __PACKAGE_NAME__.infrastructure.sample.entity.Sample;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SampleMapper extends BaseMapper<Sample> {
}
