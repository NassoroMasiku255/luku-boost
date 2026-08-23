package com.lukuboost.electricityloan.customer.repository;

import com.lukuboost.electricityloan.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    Optional<Customer> findByCustomerRef(String customerRef);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT c FROM Customer c WHERE " +
            "(:phone IS NULL OR c.phoneNumber LIKE %:phone%) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:name IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Customer> searchCustomers(
            @Param("phone") String phone,
            @Param("status") String status,
            @Param("name") String name,
            Pageable pageable);

    long countByStatus(String status);
}
