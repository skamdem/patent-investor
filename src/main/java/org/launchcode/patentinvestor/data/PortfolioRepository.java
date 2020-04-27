package org.launchcode.patentinvestor.data;

import org.launchcode.patentinvestor.models.Portfolio;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by kamdem
 */
@Repository
public interface PortfolioRepository extends CrudRepository<Portfolio, Integer> {
}
