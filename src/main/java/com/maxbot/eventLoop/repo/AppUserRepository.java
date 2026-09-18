package com.maxbot.eventLoop.repo;

import com.maxbot.eventLoop.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByMaxUserId(long maxUserId);

    /**
     * Условное списание: баланс уменьшается только если его хватает.
     * Read-modify-write здесь дал бы гонку при двойном нажатии, а проверка
     * количества изменённых строк говорит, состоялось ли списание.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE app_user
               SET balance_rub = balance_rub - :price,
                   cinema_spent_rub = cinema_spent_rub + (CASE WHEN :cinema THEN :price ELSE 0 END),
                   updated_at = now()
             WHERE id = :userId
               AND balance_rub >= :price
            """, nativeQuery = true)
    int chargeIfEnough(@Param("userId") Long userId,
                       @Param("price") int price,
                       @Param("cinema") boolean cinema);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE app_user
               SET balance_rub = balance_rub + :price,
                   cinema_spent_rub = greatest(0, cinema_spent_rub - (CASE WHEN :cinema THEN :price ELSE 0 END)),
                   updated_at = now()
             WHERE id = :userId
            """, nativeQuery = true)
    int refund(@Param("userId") Long userId,
               @Param("price") int price,
               @Param("cinema") boolean cinema);
}
