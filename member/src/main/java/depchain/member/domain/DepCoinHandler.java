package depchain.member.domain;

import depchain.common.DCLogger;
import depchain.common.domain.Account;
import depchain.common.domain.BlockChainState;
import depchain.common.domain.Transaction;
import depchain.contract.ContractFunctions;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.EVMExecutor;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class DepCoinHandler {

	public static boolean handleTransaction(Transaction tx, BlockChainState state, DCLogger dcLogger) {
		switch (tx.getTransactionType()) {
			case TRANSFER:
				return handleTransfer(tx, state, dcLogger);
			case TRANSFER_FROM:
				return handleTransferFrom(tx, state, dcLogger);
			case APPROVE:
				return handleApprove(tx, state, dcLogger);
			default:
				System.err.println("Unknown transaction type: " + tx.getTransactionType());
				return false;
		}
	}

	public static boolean handleTransferFrom(Transaction tx, BlockChainState state, DCLogger dcLogger) {
		BigInteger amount = tx.getAmount();
		Account ownerAccount = state.getAccount(tx.getSender());
		Account spenderAccount = state.getAccount(tx.getSpender());

		if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
			dcLogger.log("Invalid value");
			return false;
		}
		// spending allowance before transfer
		if (!state.spendAllowance(ownerAccount.getAddress(), spenderAccount.getAddress(), amount)) {
			dcLogger.log("Not enough allowance");
			return false;
		}
		return handleTransfer(tx, state, dcLogger);
	}

	public static boolean handleTransfer(Transaction tx, BlockChainState state, DCLogger dcLogger) {
		BigInteger amount = tx.getAmount();
		Account fromAccount = state.getAccount(tx.getSender());
		Account toAccount = state.getAccount(tx.getRecipient());

		if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
			dcLogger.log("Invalid value");
			return false;
		}
		if (fromAccount.getBalance().compareTo(amount) < 0) {
			dcLogger.log("Not enough balance");
			return false;
		}

		fromAccount.decreaseBalance(amount);
		toAccount.increaseBalance(amount);
		dcLogger.verbose("New balances:" );
		dcLogger.verbose(fromAccount + ": " + fromAccount.getBalance());
		dcLogger.verbose(toAccount + ": " + toAccount.getBalance());
		return true;
	}

	public static BigInteger handleBalance(Address addr, BlockChainState state, DCLogger dcLogger) {
		Account account = state.getAccount(addr.toHexString());
		if (account == null) {
			dcLogger.log("Account not found");
			return BigInteger.ZERO;
		}
		return account.getBalance();
	}
	
	public static BigInteger handleAllowance(Address owner, Address spender, BlockChainState state, DCLogger dcLogger) {
		Account ownerAccount = state.getAccount(owner.toHexString());
		Account spenderAccount = state.getAccount(spender.toHexString());

		if (ownerAccount == null || spenderAccount == null) {
			dcLogger.log("Account not found");
			return BigInteger.ZERO;
		}

		BigInteger allowance = state.getAllowance(ownerAccount.getName(), spenderAccount.getName());
		dcLogger.verbose("Allowance from " + owner + " to " + spender + ": " + allowance);
		return allowance;
	}

	public static boolean handleApprove(Transaction tx, BlockChainState state, DCLogger dcLogger) {
		Account ownerAccount = state.getAccount(tx.getSender());
		Account spenderAccount = state.getAccount(tx.getRecipient());
		BigInteger value = tx.getAmount();

		if (ownerAccount == null || spenderAccount == null) {
			dcLogger.log("Account not found");
			return false;
		}
		if (value == null || value.compareTo(BigInteger.ZERO) <= 0) {
			dcLogger.log("Invalid value");
			return false;
		}
		state.addAllowanceToOwner(ownerAccount.getName(), spenderAccount.getName(), value);
		dcLogger.verbose("Allowance set from " + ownerAccount + " to " + spenderAccount + ": " + value);
		return true;
	}

}
