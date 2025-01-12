package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
public class DatabaseConduit {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRecordRepository;
    private final RestTemplate restTemplate; // Used to call the Incentive API

    @Autowired
    public DatabaseConduit(UserRepository userRepository,
                           TransactionRepository transactionRecordRepository,
                           RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.restTemplate = restTemplate;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    @Transactional
    public void save(Transaction transaction) {
        if (!isValid(transaction)) {
            return; // Discard invalid transactions
        }

        UserRecord sender = queryUser(transaction.getSenderId());
        UserRecord recipient = queryUser(transaction.getRecipientId());

        // Call the Incentive API to get the incentive
        Incentive incentive = getIncentiveFromApi(transaction);
        if (incentive != null) {
            // Update balances
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive.getAmount());

            // Save updated user records
            userRepository.save(sender);
            userRepository.save(recipient);

            // Record the transaction with the incentive
            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentive.getAmount());
            transactionRecordRepository.save(transactionRecord);
        }
    }

    public boolean isValid(Transaction transaction) {
        UserRecord sender = queryUser(transaction.getSenderId());
        if (sender == null) {
            return false; // Invalid sender
        }

        UserRecord recipient = queryUser(transaction.getRecipientId());
        if (recipient == null) {
            return false; // Invalid recipient
        }

        return sender.getBalance() >= transaction.getAmount();
    }

    public UserRecord queryUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    private Incentive getIncentiveFromApi(Transaction transaction) {
        try {
            // Define the API endpoint and make the POST request to the Incentive API
            String apiUrl = "http://localhost:8080/incentive";  // Adjust the URL if necessary
            Incentive incentive = restTemplate.postForObject(apiUrl, transaction, Incentive.class);
            return incentive;
        } catch (Exception e) {
            // Log and handle any potential errors when calling the Incentive API
            e.printStackTrace();
            return null;
        }
    }
}