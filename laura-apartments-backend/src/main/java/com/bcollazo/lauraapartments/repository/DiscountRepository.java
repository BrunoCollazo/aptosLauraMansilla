package com.bcollazo.lauraapartments.repository;

import com.bcollazo.lauraapartments.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findAllByOrderByMinNightsDesc();
}