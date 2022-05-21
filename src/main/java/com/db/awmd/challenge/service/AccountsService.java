package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountDoesNotExistException;
import com.db.awmd.challenge.exception.AmountTransferShouldBeGreaterThanZero;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.SameAccountNumberException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  
  
  private final NotificationService notificationService;

  
  @Autowired
  public AccountsService(AccountsRepository accountsRepository,NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService=notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Transfer money and send notification
   * @param fromAccountId
   * @param toAccountId
   * @param amount
   * @throws Exception
   */
  public void initiateTransfer(String fromAccountId, String toAccountId, BigDecimal amount)  {
	  transferAmount(fromAccountId, toAccountId, amount);	  
	  notificationService.notifyAboutTransfer(accountsRepository.getAccount(toAccountId),"Amount "+amount+" has been transffered from "+fromAccountId+" to "+toAccountId);
	  
  }
  
  /**
   * Transfer money between two accounts thread safe manner
   * @param fromAccountId
   * @param toAccountId
   * @param amount
   * @throws Exception
   */
  public void transferAmount(String fromAccountId, String toAccountId, BigDecimal amount) {
		

	  
		// check if account exist and account no are not same 
		validateAccounts(fromAccountId, toAccountId,amount);
		
	  
	  //higher account will be set as level1
	  String level1,level2;
	  if(fromAccountId.compareTo(toAccountId)>0) {
		  level1=fromAccountId;
		  level2=toAccountId;
	  }else {
		  level1=toAccountId;
		  level2=fromAccountId;
	  }
	  
	  // always lock higher account first to avoid deadlock 
	   
	  synchronized (level1) {		  
		  synchronized (level2) {			
			  Account fromAccount =getAccount(fromAccountId);
				Account toAccount =getAccount(toAccountId);		
				BigDecimal fromAccountBalance=fromAccount.getBalance();
				if(fromAccountBalance.compareTo(amount)>=0) {			
					fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
					toAccount.setBalance(toAccount.getBalance().add(amount));		
				}else {
					throw new InsufficientBalanceException("Insufficient Balance in account "+fromAccount.getAccountId());
				}				  
		 }
		  
	  }
	  
	  
  }
  
  /**
   * 
   * @param fromAccount
   * @param toAccount
   */
  private void validateAccounts(String fromAccountId, String toAccountId,BigDecimal amount) {
	  
	  
	  if(amount.compareTo(BigDecimal.ZERO)<0) {
		  throw new AmountTransferShouldBeGreaterThanZero("Amount to be transfered should be greater than Zero");
	  }
	  
	  Account fromAccount =getAccount(fromAccountId);
	  Account toAccount =getAccount(toAccountId);		
			
	  
	  if(fromAccount==null) {
			throw new AccountDoesNotExistException("Account for account id "+fromAccountId+" does not exists");
		}
		if(toAccount==null) {
			throw new AccountDoesNotExistException("Account for account id "+toAccountId+" does not exists");
		}
	  
	  
	  if(fromAccountId.equals(toAccountId)) {
		  throw new SameAccountNumberException("Both account number are same");
	  }
	  
  }
  
  
}
