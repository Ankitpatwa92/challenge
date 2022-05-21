package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.SameAccountNumberException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  
  @MockBean
  NotificationService notificationService;
  

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }
  
  @Test
  public void initiateTransfer_twoDiffAccount() throws Exception {	  
	  String acId1="Id-" + System.currentTimeMillis();
	  Thread.sleep(1);
	  String acid2="Id-" + System.currentTimeMillis();
	  Account account1 = new Account(acId1,new BigDecimal(2000));
	  Account account2 = new Account(acid2,new BigDecimal(1000));
	  this.accountsService.createAccount(account1);
	  this.accountsService.createAccount(account2);  
	  accountsService.initiateTransfer(acId1, acid2, new BigDecimal(500));
	  assertEquals("From account balance is not as expected",new BigDecimal(1500), account1.getBalance());
	  assertEquals("To account balance is not as expected",new BigDecimal(1500), account2.getBalance());
  }

  @Test
  public void initiateTransfer_insufficientBalance() throws Exception {	  
	  String acId1="Id-" + System.currentTimeMillis();
	  Thread.sleep(1);
	  String acid2="Id-" + System.currentTimeMillis();
	  Account account1 = new Account(acId1,new BigDecimal(499));
	  Account account2 = new Account(acid2,new BigDecimal(1000));
	  this.accountsService.createAccount(account1);
	  this.accountsService.createAccount(account2);  
	  try {
		accountsService.initiateTransfer(acId1, acid2, new BigDecimal(500));
		fail("Should have failed when insufficient balance");
	} catch (InsufficientBalanceException e) {
		assertThat(e.getMessage().equals("Insufficient Balance in account "+acId1));
	}
  }

  
  @Test
  public void initiateTransfer_sameAccount() throws Exception {	  
	  String acId1="Id-" + System.currentTimeMillis();
	  Account account1 = new Account(acId1,new BigDecimal(2000));
	  this.accountsService.createAccount(account1);	 
	  try {
		accountsService.initiateTransfer(acId1, acId1, new BigDecimal(500));
		fail("Should have failed when transfer amount from same accunt");
	} catch (SameAccountNumberException e) {
		assertThat(e.getMessage().equals("Both account number are same"));
	}
  }

  /**
   * Money is transfered between accounts through parallel processing 
   * Transfer is made such that after all transfer account balance should be same  
   * @throws Exception
   */
  @Test
  public void initiateTransfer_balanceConsistency() throws Exception {	  
	  String acId1="Id-" + System.currentTimeMillis();
	  Thread.sleep(1);
	  String acId2="Id-" + System.currentTimeMillis();
	  Thread.sleep(1);
	  String acId3="Id-" + System.currentTimeMillis();
	  Thread.sleep(1);
	  String acId4="Id-" + System.currentTimeMillis();
	  
	  Account account1 = new Account(acId1,new BigDecimal(200000));
	  Account account2 = new Account(acId2,new BigDecimal(200000));
	  Account account3 = new Account(acId3,new BigDecimal(200000));
	  Account account4 = new Account(acId4,new BigDecimal(200000));
	  this.accountsService.createAccount(account1);
	  this.accountsService.createAccount(account2);
	  this.accountsService.createAccount(account3);
	  this.accountsService.createAccount(account4);
	 	 
	  
	  ExecutorService executorService = Executors.newFixedThreadPool(10);
	  List<Callable<String>> tasks=new ArrayList<>();
	        
	  for(int i=0;i<100;i++) {
		  tasks.add(getTask(acId1, acId2, new BigDecimal(200)));
		  tasks.add(getTask(acId2, acId1, new BigDecimal(200)));
	  }
	  
	  	  
	  tasks.add(getTask(acId1, acId3, new BigDecimal(500)));
	  tasks.add(getTask(acId2, acId4, new BigDecimal(500)));
	  tasks.add(getTask(acId4, acId1, new BigDecimal(500)));
	  tasks.add(getTask(acId3, acId2, new BigDecimal(500)));
	  
	  
	  
	  List<Future<String>> futures = executorService.invokeAll(tasks);			
	  futures.forEach(f->f.isDone());
	  
	  assertEquals("Account1 balance is not correct",new BigDecimal(200000), account1.getBalance());
	  assertEquals("Account2 balance is not correct",new BigDecimal(200000), account2.getBalance());
	  assertEquals("Account3 balance is not correct",new BigDecimal(200000), account3.getBalance());
	  assertEquals("Account4 balance is not correct",new BigDecimal(200000), account4.getBalance());

	  
  }

  private Callable<String> getTask(String acId1,String acId2,BigDecimal amount) {
	  return new Callable<String>() {
		
		@Override
		public String call() throws Exception {
			accountsService.initiateTransfer(acId1, acId2, amount);
			return "SUCCESS";
		}
	};
	
  }
  
  
  
}
