package com.example.tennis_club.dao;

import com.example.tennis_club.entities.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerDaoTest extends DaoUtils {

    @Autowired
    private CustomerDao customerDao;

    @Test
    void saveShouldPersistNewCustomerAndAssignId() {
        Customer customer = new Customer();
        customer.setPhoneNumber("+421900111222");
        customer.setName("Peter Novak");

        Customer savedCustomer = customerDao.save(customer);

        flushAndClear();

        assertThat(savedCustomer.getId()).isNotNull();

        Customer persistedCustomer = entityManager.find(Customer.class, savedCustomer.getId());
        assertThat(persistedCustomer).isNotNull();
        assertThat(persistedCustomer.getPhoneNumber()).isEqualTo("+421900111222");
        assertThat(persistedCustomer.getName()).isEqualTo("Peter Novak");
        assertThat(persistedCustomer.isDeleted()).isFalse();
    }

    @Test
    void saveShouldMergeDetachedExistingCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Customer detachedCustomer = entityManager.find(Customer.class, customer.getId());
        entityManager.detach(detachedCustomer);

        detachedCustomer.setName("Updated Peter");

        Customer updatedCustomer = customerDao.save(detachedCustomer);

        flushAndClear();

        Customer persistedCustomer = entityManager.find(Customer.class, updatedCustomer.getId());
        assertThat(persistedCustomer.getName()).isEqualTo("Updated Peter");
        assertThat(persistedCustomer.getPhoneNumber()).isEqualTo("+421900111222");
    }

    @Test
    void findByIdShouldReturnActiveCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Optional<Customer> result = customerDao.findById(customer.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Peter Novak");
    }

    @Test
    void findByIdShouldReturnSoftDeletedCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        customer.setDeleted(true);
        flushAndClear();

        Optional<Customer> result = customerDao.findById(customer.getId());

        assertThat(result).isPresent();
        assertThat(result.get().isDeleted()).isTrue();
    }

    @Test
    void findActiveByIdShouldReturnActiveCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Optional<Customer> result = customerDao.findActiveById(customer.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPhoneNumber()).isEqualTo("+421900111222");
    }

    @Test
    void findActiveByIdShouldReturnEmptyForSoftDeletedCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        customer.setDeleted(true);
        flushAndClear();

        Optional<Customer> result = customerDao.findActiveById(customer.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByIdShouldReturnEmptyForUnknownId() {
        Optional<Customer> result = customerDao.findActiveById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllActiveShouldReturnOnlyActiveCustomers() {
        Customer peter = createCustomer("+421900111222", "Peter Novak");
        Customer martin = createCustomer("+421900333444", "Martin Hrasko");
        Customer deleted = createCustomer("+421900555666", "Deleted Customer");
        deleted.setDeleted(true);
        flushAndClear();

        List<Customer> result = customerDao.findAllActive();

        assertThat(result)
                .extracting(Customer::getId)
                .containsExactlyInAnyOrder(peter.getId(), martin.getId())
                .doesNotContain(deleted.getId());
    }

    @Test
    void findActiveByPhoneNumberShouldReturnCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Optional<Customer> result = customerDao.findActiveByPhoneNumber("+421900111222");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(customer.getId());
        assertThat(result.get().getName()).isEqualTo("Peter Novak");
    }

    @Test
    void findActiveByPhoneNumberShouldReturnEmptyForUnknownPhoneNumber() {
        createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Optional<Customer> result = customerDao.findActiveByPhoneNumber("+421900999888");

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByPhoneNumberShouldReturnEmptyForSoftDeletedCustomer() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        customer.setDeleted(true);
        flushAndClear();

        Optional<Customer> result = customerDao.findActiveByPhoneNumber("+421900111222");

        assertThat(result).isEmpty();
    }

    @Test
    void softDeleteShouldMarkCustomerAsDeletedAndExcludeItFromActiveQueries() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Customer existingCustomer = entityManager.find(Customer.class, customer.getId());
        customerDao.softDelete(existingCustomer);

        flushAndClear();

        assertThat(customerDao.findById(customer.getId())).isPresent();
        assertThat(customerDao.findActiveById(customer.getId())).isEmpty();
        assertThat(customerDao.findActiveByPhoneNumber("+421900111222")).isEmpty();
        assertThat(customerDao.findAllActive()).isEmpty();
    }

    @Test
    void saveShouldRejectDuplicatePhoneNumber() {
        createCustomer("+421900111222", "Peter Novak");
        flushAndClear();

        Customer duplicate = new Customer();
        duplicate.setPhoneNumber("+421900111222");
        duplicate.setName("Another Customer");

        assertThrows(DataIntegrityViolationException.class, () -> customerDao.save(duplicate));
    }

    @Test
    void saveShouldRejectNullPhoneNumber() {
        Customer customer = new Customer();
        customer.setName("Peter Novak");

        assertThrows(DataIntegrityViolationException.class, () -> customerDao.save(customer));
    }

    @Test
    void saveShouldRejectNullName() {
        Customer customer = new Customer();
        customer.setPhoneNumber("+421900111222");

        assertThrows(DataIntegrityViolationException.class, () -> customerDao.save(customer));
    }
}