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

        LOGGER.info("Generating statement for account {} from {} to {}", 
                    accountId, startDate, endDate);

        // Get opening balance as of the start date
        Long openingBalance = transactionRepository.getBalanceAsOf(accountId, routingNum, startDate);
        LOGGER.debug("Opening balance for account {} as of {}: {}", 
                    accountId, startDate, openingBalance);

        // Get transactions for that period
        List<Transaction> transactions = transactionRepository.findTransactionsForDateRange(accountId, routingNum, startDate, endDate);
        LOGGER.debug("Found {} transactions for account {} in date range", 
                    transactions.size(), accountId);

        // Calculate total deposits and withdrawals for the period
        Long totalDeposits = 0L; // Initialize total deposits to 0
        Long totalWithdrawals = 0L; // Initialize total withdrawals to 0

        // Replace the existing transaction calculation loop with this:
        for (Transaction transaction : transactions) {
            if (transaction.getToAccountNum().equals(accountId) && 
                transaction.getToRoutingNum().equals(routingNum)) {
                // This is a deposit/credit to the account
                totalDeposits += transaction.getAmount();
                LOGGER.debug("Added deposit: {} for transaction {}", 
                            transaction.getAmount(), transaction.getTransactionId());
            } else if (transaction.getFromAccountNum().equals(accountId) && 
                       transaction.getFromRoutingNum().equals(routingNum)) {
                // This is a withdrawal/debit from the account
                totalWithdrawals += transaction.getAmount();
                LOGGER.debug("Added withdrawal: {} for transaction {}", 
                            transaction.getAmount(), transaction.getTransactionId());
            }
        }
        LOGGER.debug("Total deposits: {}, Total withdrawals: {}", 
                    totalDeposits, totalWithdrawals);

        // Calculate closing balance as of the end date
        Long closingBalance = transactionRepository.getBalanceAsOf(accountId, routingNum, endDate);
        LOGGER.debug("Closing balance for account {} as of {}: {}", 
                    accountId, endDate, closingBalance);

        // Verify balance calculation accuracy
        Long calculatedClosingBalance = openingBalance + totalDeposits - totalWithdrawals;
        if (!calculatedClosingBalance.equals(closingBalance)) {
            LOGGER.warn("Balance calculation discrepancy: Database closing balance is {}, but calculated balance is {}",
                       closingBalance, calculatedClosingBalance);
            // Use the calculated balance instead to ensure accuracy
            closingBalance = calculatedClosingBalance;
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