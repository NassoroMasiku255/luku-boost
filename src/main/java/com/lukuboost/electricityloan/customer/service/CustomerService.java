package com.lukuboost.electricityloan.customer.service;

import com.lukuboost.electricityloan.common.utility.ReferenceGenerator;
import com.lukuboost.electricityloan.customer.entity.Customer;
import com.lukuboost.electricityloan.customer.repository.CustomerRepository;
import com.lukuboost.electricityloan.meter.entity.Meter;
import com.lukuboost.electricityloan.meter.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;

    @Transactional
    public Customer findOrCreateCustomer(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    Customer customer = Customer.builder()
                            .customerRef(ReferenceGenerator.generateCustomerReference())
                            .phoneNumber(phoneNumber)
                            .status("ACTIVE")
                            .build();
                    customer = customerRepository.save(customer);
                    log.info("Created new customer ref={}", customer.getCustomerRef());
                    return customer;
                });
    }

    @Transactional
    public Meter findOrCreateMeter(String meterNumber, Customer customer) {
        return meterRepository.findByMeterNumber(meterNumber)
                .orElseGet(() -> {
                    Meter meter = Meter.builder()
                            .meterNumber(meterNumber)
                            .customer(customer)
                            .status("ACTIVE")
                            .build();
                    meter = meterRepository.save(meter);
                    log.info("Created new meter number={}", meterNumber);
                    return meter;
                });
    }

    public Page<Customer> searchCustomers(String phone, String status, String name, Pageable pageable) {
        return customerRepository.searchCustomers(phone, status, name, pageable);
    }

    public Customer getById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new com.lukuboost.electricityloan.common.exception.ResourceNotFoundException("Customer", id.toString()));
    }

    public Customer getByRef(String ref) {
        return customerRepository.findByCustomerRef(ref)
                .orElseThrow(() -> new com.lukuboost.electricityloan.common.exception.ResourceNotFoundException("Customer", ref));
    }

    public Customer getByPhone(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new com.lukuboost.electricityloan.common.exception.ResourceNotFoundException("Customer", phoneNumber));
    }
}
