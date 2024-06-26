package itss.group22.bookexchangeeasy.repository;

import itss.group22.bookexchangeeasy.entity.Transaction;
import itss.group22.bookexchangeeasy.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.owner.id = ?1 OR t.borrower.id = ?1 " +
            "ORDER BY t.timestamp DESC")
    Page<Transaction> findByUserOrderByTimestampDesc(Long userId, Pageable pageable);

    List<Transaction> findByTargetBookId(Long bookId);

    long countByStatus(TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE YEAR(t.timestamp) = ?1 AND MONTH(t.timestamp) = ?2 AND DAY(t.timestamp) = ?3")
    Long countByDate(int year, int month, int date);
    @Query("SELECT COUNT(t) FROM Transaction t WHERE YEAR(t.timestamp) = ?1 AND MONTH(t.timestamp) = ?2")
    Long countByMonth(int year, int month);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.id LIKE CONCAT('%', LOWER(?1), '%') " +
            "OR LOWER(t.owner.email) LIKE CONCAT('%', LOWER(?1), '%') " +
            "OR LOWER(t.borrower.email) LIKE CONCAT('%', LOWER(?1), '%') " +
            "OR LOWER(t.targetBook.title) LIKE CONCAT('%', LOWER(?1), '%') " +
            "ORDER BY t.timestamp DESC")
    Page<Transaction> findByIdOrOwnerEmailOrBorrowerEmailOrBookTitleLike(String keyword, Pageable pageable);
}
