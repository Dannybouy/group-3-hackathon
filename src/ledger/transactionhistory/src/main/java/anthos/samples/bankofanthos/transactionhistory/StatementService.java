package anthos.samples.bankofanthos.transactionhistory;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Service // Indicates that this class is a service
public class StatementService {

    private static final Logger LOGGER = LogManager.getLogger(StatementService.class);

    @Autowired // automatically connects to the TransactionRepository
    private TransactionRepository transactionRepository;

    public BankStatement generateStatement(
            String accountId,
            String userName,
            String routingNum,
            Date startDate,
            Date endDate) {

        // Validate input parameters
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (routingNum == null || routingNum.isEmpty()) {
            throw new IllegalArgumentException("Routing number cannot be null or empty");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        LOGGER.info("Generating statement for account {} from {} to {}", 
                    accountId, startDate, endDate);

        // Get opening balance as of the start date
        Long openingBalance;
        try {
            openingBalance = transactionRepository.getBalanceAsOf(accountId, routingNum, startDate);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve opening balance for account {}: {}", accountId, e.getMessage());
            throw new RuntimeException("Error retrieving opening balance", e);
        }
        LOGGER.debug("Opening balance for account {} as of {}: {}", 
                    accountId, startDate, openingBalance);

        // Get transactions for that period
        List<Transaction> transactions;
        try {
            transactions = transactionRepository.findTransactionsForDateRange(accountId, routingNum, startDate, endDate);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve transactions for account {}: {}", accountId, e.getMessage());
            throw new RuntimeException("Error retrieving transactions", e);
        }
        if (transactions == null) {
            transactions = List.of(); // Use an empty list if no transactions are found
        }
        LOGGER.debug("Found {} transactions for account {} in date range", 
                    transactions.size(), accountId);

        // Calculate total deposits and withdrawals for the period
        Long totalDeposits = 0L; // Initialize total deposits to 0
        Long totalWithdrawals = 0L; // Initialize total withdrawals to 0

        for (Transaction transaction : transactions) {
            if (transaction.getToAccountNum().equals(accountId)) {
                if (totalDeposits > Long.MAX_VALUE - transaction.getAmount()) {
                    throw new ArithmeticException("Total deposits would overflow");
                }
                totalDeposits += transaction.getAmount();
            } else {
                if (totalWithdrawals > Long.MAX_VALUE - transaction.getAmount()) {
                    throw new ArithmeticException("Total withdrawals would overflow");
                }
                totalWithdrawals += transaction.getAmount();
            }
        }
        LOGGER.debug("Total deposits: {}, Total withdrawals: {}", 
                    totalDeposits, totalWithdrawals);

        // Calculate closing balance as of the end date
        Long closingBalance;
        try {
            closingBalance = transactionRepository.getBalanceAsOf(accountId, routingNum, endDate);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve closing balance for account {}: {}", accountId, e.getMessage());
            throw new RuntimeException("Error retrieving closing balance", e);
        }
        LOGGER.debug("Closing balance for account {} as of {}: {}", 
                    accountId, endDate, closingBalance);

        // Verify balance calculation accuracy
        Long calculatedClosingBalance = openingBalance + totalDeposits - totalWithdrawals;
        if (!calculatedClosingBalance.equals(closingBalance)) {
            LOGGER.error("Critical balance mismatch detected: Database balance={}, Calculated balance={}", 
                         closingBalance, calculatedClosingBalance);
            throw new IllegalStateException("Balance mismatch detected. Investigation required.");
        }

        // Create a new BankStatement object
        BankStatement statement = new BankStatement(
            accountId,
            userName,
            startDate,
            endDate,
            openingBalance,
            closingBalance,
            transactions,
            totalDeposits,
            totalWithdrawals
        );

        return statement;
    }
}