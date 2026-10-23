package com.collabflow.platform.repository;

import com.collabflow.platform.model.FeatureFlag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    List<FeatureFlag> findAllByOrderByKeyAsc();
}
