package org.example.pim_system.repository;

import org.example.pim_system.model.Employee;
import org.example.pim_system.model.MonthlySalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlySalaryRepository extends JpaRepository<MonthlySalary, Long> {
    Optional<MonthlySalary> findByEmployeeAndSalaryMonth(Employee employee, String salaryMonth);
    List<MonthlySalary> findBySalaryMonthOrderByEmployee_FullNameAsc(String salaryMonth);
}
