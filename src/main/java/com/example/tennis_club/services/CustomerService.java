package com.example.tennis_club.services;

import com.example.tennis_club.daos.CustomerDao;
import com.example.tennis_club.entities.Customer;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// This is just simpler internal service so there is no need for dtos in my opinion to reduce boilerplate.

@Service
@Transactional
public class CustomerService {
    private final CustomerDao customerDao;

    public CustomerService(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findActiveCustomerByPhoneNumber(String phoneNumber) {
        return customerDao.findActiveByPhoneNumber(phoneNumber);
    }

    @Transactional(readOnly = true)
    public Customer getActiveCustomerByPhoneNumber(String phoneNumber) {
        return findActiveCustomerByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    public Customer resolveCustomer(String phoneNumber, String name) {
        return findActiveCustomerByPhoneNumber(phoneNumber)
                .map(existingCustomer -> validateExistingCustomerName(existingCustomer, name))
                .orElseGet(() -> createCustomer(phoneNumber, name));
    }

    private Customer validateExistingCustomerName(Customer customer, String requestedName) {
        if (!customer.getName().equals(requestedName)) {
            throw new BadRequestException("Customer name does not match phone number");
        }

        return customer;
    }

    private Customer createCustomer(String phoneNumber, String name) {
        Customer customer = new Customer();
        customer.setPhoneNumber(phoneNumber);
        customer.setName(name);

        return customerDao.save(customer);
    }
}
