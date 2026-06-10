package com.example.tennis_club.services;

import com.example.tennis_club.daos.CustomerDao;
import com.example.tennis_club.entities.Customer;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerDao customerDao;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void findActiveCustomerByPhoneNumberShouldReturnDaoResult() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        Optional<Customer> result = customerService.findActiveCustomerByPhoneNumber("+421900111222");

        assertThat(result).contains(customer);
        verify(customerDao).findActiveByPhoneNumber("+421900111222");
    }

    @Test
    void getActiveCustomerByPhoneNumberShouldReturnCustomerWhenFound() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        Customer result = customerService.getActiveCustomerByPhoneNumber("+421900111222");

        assertThat(result).isSameAs(customer);
    }

    @Test
    void getActiveCustomerByPhoneNumberShouldThrowWhenNotFound() {
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getActiveCustomerByPhoneNumber("+421900111222"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer not found");
    }

    @Test
    void resolveCustomerShouldReturnExistingCustomerWhenNameMatches() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        Customer result = customerService.resolveCustomer("+421900111222", "Peter Novak");

        assertThat(result).isSameAs(customer);
        verify(customerDao, never()).save(any());
    }

    @Test
    void resolveCustomerShouldThrowWhenPhoneNumberBelongsToDifferentName() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.resolveCustomer("+421900111222", "Martin Hrasko"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Customer name does not match phone number");

        verify(customerDao, never()).save(any());
    }

    @Test
    void resolveCustomerShouldCreateCustomerWhenPhoneNumberDoesNotExist() {
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.empty());
        when(customerDao.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = customerService.resolveCustomer("+421900111222", "Peter Novak");

        assertThat(result.getPhoneNumber()).isEqualTo("+421900111222");
        assertThat(result.getName()).isEqualTo("Peter Novak");
        verify(customerDao).save(any(Customer.class));
    }

    private Customer customer(String phoneNumber, String name) {
        Customer customer = new Customer();
        customer.setPhoneNumber(phoneNumber);
        customer.setName(name);
        return customer;
    }

    @Test
    void resolveCustomerShouldCreateCustomerWithDeletedFalseByDefault() {
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.empty());
        when(customerDao.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = customerService.resolveCustomer("+421900111222", "Peter Novak");

        assertThat(result.isDeleted()).isFalse();
    }

    @Test
    void resolveCustomerShouldPassNewCustomerToDaoWithExpectedValues() {
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.empty());
        when(customerDao.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.resolveCustomer("+421900111222", "Peter Novak");

        verify(customerDao).save(argThat(customer ->
                customer.getPhoneNumber().equals("+421900111222")
                        && customer.getName().equals("Peter Novak")
                        && !customer.isDeleted()
        ));
    }

    @Test
    void resolveCustomerShouldNotCreateCustomerWhenExistingCustomerIsFound() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        customerService.resolveCustomer("+421900111222", "Peter Novak");

        verify(customerDao, never()).save(any(Customer.class));
    }

    @Test
    void resolveCustomerShouldReturnSameExistingCustomerInstance() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        Customer result = customerService.resolveCustomer("+421900111222", "Peter Novak");

        assertThat(result).isSameAs(customer);
    }

    @Test
    void resolveCustomerShouldUsePhoneNumberLookupBeforeCreatingCustomer() {
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.empty());
        when(customerDao.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.resolveCustomer("+421900111222", "Peter Novak");

        var inOrder = inOrder(customerDao);
        inOrder.verify(customerDao).findActiveByPhoneNumber("+421900111222");
        inOrder.verify(customerDao).save(any(Customer.class));
    }

    @Test
    void resolveCustomerShouldNotSaveWhenNameDoesNotMatchExistingCustomer() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.resolveCustomer("+421900111222", "Martin Hrasko"))
                .isInstanceOf(BadRequestException.class);

        verify(customerDao, never()).save(any(Customer.class));
    }

    @Test
    void getActiveCustomerByPhoneNumberShouldDelegateToDaoWithGivenPhoneNumber() {
        Customer customer = customer("+421900111222", "Peter Novak");
        when(customerDao.findActiveByPhoneNumber("+421900111222")).thenReturn(Optional.of(customer));

        customerService.getActiveCustomerByPhoneNumber("+421900111222");

        verify(customerDao).findActiveByPhoneNumber("+421900111222");
    }
}