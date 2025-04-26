package anthos.samples.bankofanthos.transactionhistory;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;


@Service // Indicates that this class is a service
public class StatementService {

    @Autowired // automatically connects to the TransactionRepository
    private TransactionRepository transactionRepository;

    public BankStatement generateStatement(
            String accountId,
            String userName,
            String routingNum,
            Date startDate,
            Date endDate) {

        // Get opening balance as of the start date
        Long openingBalance = transactionRepository.getBalanceAsOf(accountId, routingNum, startDate);

        // Get transactions for that period
        List<Transaction> transactions = transactionRepository.findTransactionsForDateRange(accountId, routingNum, startDate, endDate);

        // Calculate total deposits and withdrawals for the period
        Long totalDeposits = 0L; // Initialize total deposits to 0
        Long totalWithdrawals = 0L; // Initialize total withdrawals to 0

        for (Transaction transaction : transactions) {
            if (transaction.getToAccountNum().equals(accountId)) {
                totalDeposits += transaction.getAmount();
            } else {
                totalWithdrawals += transaction.getAmount();
            }
        }

        // Calculate closing balance as of the end date
        Long closingBalance = transactionRepository.getBalanceAsOf(accountId, routingNum, endDate);

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