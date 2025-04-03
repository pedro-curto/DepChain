package depchain.member.domain;

import depchain.common.domain.Transaction;
import depchain.contract.ContractFunctions;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.EVMExecutor;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class ISTCoinHandler {

	public static boolean handleTransaction(Transaction tx, EVMExecutor evmExecutor, ByteArrayOutputStream bos) {
		switch (tx.getTransactionType()) {
			case TRANSFER:
				return handleTransfer(tx, evmExecutor, bos);
			case TRANSFER_FROM:
				return handleTransferFrom(tx, evmExecutor, bos);
			case APPROVE:
				return handleApprove(tx, evmExecutor, bos);
			default:
				System.err.println("Unknown transaction type: " + tx.getTransactionType());
				return false;
		}
	}

	private static boolean handleTransfer(Transaction tx, EVMExecutor evmExecutor, ByteArrayOutputStream bos) {
		Address from = Address.fromHexString(tx.getSender());
		Address to = Address.fromHexString(tx.getRecipient());
		BigInteger value = tx.getAmount();
		return ContractFunctions.transferTokens(evmExecutor, bos, from, to, value);
	}

	private static boolean handleTransferFrom(Transaction tx, EVMExecutor evmExecutor, ByteArrayOutputStream bos) {
		System.err.println("Handling transfer from");
		Address spender = Address.fromHexString(tx.getSpender());
		Address from = Address.fromHexString(tx.getSender());
		Address to = Address.fromHexString(tx.getRecipient());
		return ContractFunctions.transferFrom(evmExecutor, bos, spender, from, to, tx.getAmount());
	}

	private static boolean handleApprove(Transaction tx, EVMExecutor evmExecutor, ByteArrayOutputStream bos) {
		Address owner = Address.fromHexString(tx.getSender());
		Address spender = Address.fromHexString(tx.getRecipient());
		return ContractFunctions.approve(evmExecutor, bos, owner, spender, tx.getAmount());
	}

	public static BigInteger handleBalance(Address addr, EVMExecutor evmExecutor, ByteArrayOutputStream bos) {
		return ContractFunctions.callBalanceOf(evmExecutor, bos, addr);
	}

	public static BigInteger handleAllowance(Address owner, Address spender, EVMExecutor evmExecutor, ByteArrayOutputStream bos) {
		return ContractFunctions.callAllowance(evmExecutor, bos, owner, spender);
	}
}
