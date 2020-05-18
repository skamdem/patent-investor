package org.launchcode.patentinvestor.data;

import org.launchcode.patentinvestor.models.StockShare;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by kamdem
 */
@Repository
public interface StockShareRepository extends CrudRepository<StockShare, Integer> {
}
