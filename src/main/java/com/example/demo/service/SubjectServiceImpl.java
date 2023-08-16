package com.example.demo.service;

import com.example.demo.api.CreateSubjectRequest;
import com.example.demo.api.SubjectResponse;
import com.example.demo.client.PrefixClient;
import com.example.demo.domain.BankAccount;
import com.example.demo.domain.Subject;
import com.example.demo.mapper.SubjectMapper;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.repository.SequenceProvider;
import com.example.demo.repository.SubjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class SubjectServiceImpl implements SubjectService {

  private final SubjectMapper subjectMapper;
  private final SubjectRepository subjectRepository;
  private final BankAccountRepository bankAccountRepository;
  private final PrefixClient prefixClient;
  private SequenceProvider sequenceProvider; // SequenceProvider implementation


  // Dependency Injection Constructor
  @Autowired
  public SubjectServiceImpl(SequenceProvider sequenceProvider,
                            PrefixClient prefixClient,
                            SubjectMapper subjectMapper,
                            SubjectRepository subjectRepository,
                            BankAccountRepository bankAccountRepository) {
    this.sequenceProvider = sequenceProvider;
    this.prefixClient = prefixClient;
    this.subjectMapper = subjectMapper;
    this.subjectRepository = subjectRepository;
    this.bankAccountRepository = bankAccountRepository;
  }
  private BankAccount createBankAccount(String suffix, String prefixValue, Subject subject) {
    BankAccount bankAccount = new BankAccount();
    bankAccount.setSuffix(suffix);
    bankAccount.setPrefix(prefixValue);
    bankAccount.setSubject(subject);
    bankAccount.setBalance(BigDecimal.valueOf(0));
    return bankAccount;
  }

  private String fetchPrefixFromClient() {
    ResponseEntity<PrefixClient.Prefix> responseEntity = prefixClient.getPrefix();
    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      PrefixClient.Prefix prefix = responseEntity.getBody();
      return prefix.getPrefix();
    }else {
      // Error Handling
      return "Error fetching prefix";
    }
  }
  @Override
  @Transactional
  public Long save(CreateSubjectRequest request) {
    Subject subject = subjectMapper.map(request);
    subjectRepository.saveAndFlush(subject);

    String suffix = generateRandomSuffix(); // Get suffix from sequence provider
    String prefixValue = "v2"; // Get prefix from client - fetchPrefixFromClient()


    BankAccount bankAccount = createBankAccount(suffix, prefixValue, subject);
    bankAccountRepository.save(bankAccount);

    return subject.getId();

  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SubjectResponse> findById(Long id) {
    return subjectRepository.findById(id)
        .map(db -> {
          final var mapped = subjectMapper.map(db);
          mapped.setNumberOfAccounts(bankAccountRepository.numberOfAccounts(db.getId()));

          return mapped;
        });
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubjectResponse> subjectsWithLowBalance() {
    return subjectRepository.getSubjectsWithLowBalance()
        .stream()
        .map(subjectMapper::map)
        .collect(Collectors.toList());
  }

  private String generateRandomSuffix() {
    String sequence = sequenceProvider.next();
    return String.format("%04d", Integer.parseInt(sequence) % 10000);
  }



}
