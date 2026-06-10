package com.example.tennis_club.dao;

import com.example.tennis_club.entities.Customer;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CustomerDao extends BaseDao<Customer> {

    public CustomerDao() {
        super(Customer.class);
    }

    public Optional<Customer> findActiveByPhoneNumber(String phoneNumber) {
        return entityManager.createQuery("""
                        select customer
                        from Customer customer
                        where customer.phoneNumber = :phoneNumber
                          and customer.deleted = false
                        """, Customer.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultStream()
                .findFirst();
    }
}
