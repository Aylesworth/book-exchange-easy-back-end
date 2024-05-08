package itss.group22.bookexchangeeasy.repository;

import itss.group22.bookexchangeeasy.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.owner.id = ?1 OR t.borrower.id = ?1 " +
            "ORDER BY t.timestamp DESC")
    Page<Transaction> findByUserOrderByTimestampDesc(Long userId, Pageable pageable);
}
