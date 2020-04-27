package org.launchcode.patentinvestor.data;

import org.launchcode.patentinvestor.models.Tag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by kamdem
 */
@Repository
public interface TagRepository extends CrudRepository<Tag, Integer> {
}
