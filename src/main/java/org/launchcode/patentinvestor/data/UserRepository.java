package org.launchcode.patentinvestor.data;

import org.launchcode.patentinvestor.models.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by kamdem
 */
public interface UserRepository extends CrudRepository<User, Integer> {
    User findByUsername(String username);
}
