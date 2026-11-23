package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.indocrm.indocrm.model.Lead;

@Repository
public interface LeadRepository extends MongoRepository<Lead, String> {

    // ================= SEARCH =================
    @Query("{ '$or': [ " +
           "{ 'leadId': { $regex: ?0, $options: 'i' } }, " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'school': { $regex: ?0, $options: 'i' } }, " +
           "{ 'mobile': { $regex: ?0, $options: 'i' } }, " +
           "{ 'phoneNumber': { $regex: ?0, $options: 'i' } }, " +
           "{ 'email': { $regex: ?0, $options: 'i' } }, " +
           "{ 'district': { $regex: ?0, $options: 'i' } }, " +
           "{ 'schoolAddress': { $regex: ?0, $options: 'i' } }, " +
           "{ 'notes': { $regex: ?0, $options: 'i' } } " +
           "] }")
    Page<Lead> searchLeads(String searchTerm, Pageable pageable);

    // ================= FILTER =================
    Page<Lead> findByCategory(String category, Pageable pageable);
    Page<Lead> findByStatus(String status, Pageable pageable);
    Page<Lead> findByAssignedTo(String assignedTo, Pageable pageable);
    Page<Lead> findByDistrict(String district, Pageable pageable);
    Page<Lead> findByCategoryAndStatus(String category, String status, Pageable pageable);
    Page<Lead> findByCategoryAndStatusAndAssignedTo(String category, String status, String assignedTo, Pageable pageable);

    // ================= LIST =================
    List<Lead> findAllByOrderByCreatedAtDesc();
    List<Lead> findByAssignedTo(String assignedTo);
    List<Lead> findByDistrict(String district);
    List<Lead> findByDistrictAndAssignedTo(String district, String assignedTo);
    List<Lead> findByDistrictAndUploadBatch(String district, String uploadBatch);
    List<Lead> findBySchool(String school);
    List<Lead> findByStatus(String status);

    // ================= TELECALLER DISTRICT =================
    
    @Query("{ 'district': { $regex: '^?0$', $options: 'i' } }")
    List<Lead> findByDistrictIgnoreCase(String district);

    @Query("{ 'district': { $regex: ?0, $options: 'i' } }")
    List<Lead> findByDistrictRegex(String districtRegex);

    // ================= EXCEL FIELD QUERIES =================
    List<Lead> findByAlreadySoftwareUsers(String alreadySoftwareUsers);
    List<Lead> findByDemoScheduling(String demoScheduling);
    List<Lead> findByAcceptedSubscription(String acceptedSubscription);
    List<Lead> findByRejected(String rejected);
    List<Lead> findBySchoolClosed(String schoolClosed);
    List<Lead> findByIcInstalled(String icInstalled);
    List<Lead> findByAppointmentSecured(String appointmentSecured);

    // ================= SINGLE =================
    Optional<Lead> findTopByOrderByLeadIdDesc();
    Optional<Lead> findByMobile(String mobile);
    Optional<Lead> findByMobileAndDistrict(String mobile, String district);
    Optional<Lead> findByPhoneNumber(String phoneNumber);
    Optional<Lead> findByLeadId(String leadId);

    // ================= EXISTS =================
    boolean existsByMobile(String mobile);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsBySchoolAndDistrict(String school, String district);

    // ================= COUNT =================
    long count();
    long countByStatus(String status);
    long countByCategory(String category);
    long countByAssignedTo(String assignedTo);
    long countByDistrict(String district);

    // Excel field counts (for dashboard)
    long countByNoNumber(String noNumber);
    long countByNotExistNumber(String notExistNumber);
    long countByNotAnswering(String notAnswering);
    long countByTotalNumberOfAnswering(String totalNumberOfAnswering);
    long countByShareProfile(String shareProfile);
    long countByNumberOfNoNeed(String numberOfNoNeed);
    long countByAlreadySoftwareUsers(String alreadySoftwareUsers);
    long countByDemoScheduling(String demoScheduling);
    long countByAcceptedSubscription(String acceptedSubscription);
    long countByRejected(String rejected);
    long countBySchoolClosed(String schoolClosed);
    long countByIcInstalled(String icInstalled);
    long countByAppointmentSecured(String appointmentSecured);

    // TC Next Followup count (non-null)
    long countByTcNextRefollowupDateIsNotNull();
}