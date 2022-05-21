package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;
  
  @MockBean
  NotificationService notificationService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }
  
  @Test
  public void transferAmount_twoDiffAccount() throws Exception {
	  String acId1="Id-123";
	  String acId2="Id-124";	  
	  createSampleAccount(acId1,acId2,new BigDecimal(200),new BigDecimal(200));
	  mockMvc.perform(put("/v1/accounts/transferAmount/"+acId1+"/"+acId2+"/20"))
	  .andExpect(status().isOk())
	  .andExpect(content().string("SUCCESS"));
  }   
  
  @Test
  public void transferAmount_negativeAmount() throws Exception {
	  String acId1="Id-123";
	  String acId2="Id-124";	  
	  createSampleAccount(acId1,acId2,new BigDecimal(200),new BigDecimal(200));
	  mockMvc.perform(put("/v1/accounts/transferAmount/"+acId1+"/"+acId2+"/-20"))
	  .andExpect(status().isBadRequest())
	  .andExpect(content().string("Amount to be transfered should be greater than Zero"));
	  
  }   

  @Test
  public void transferAmount_twoSameAccount() throws Exception {
	  String acId1="Id-123";
	  String acId2="Id-124";	  
	  createSampleAccount(acId1,acId2,new BigDecimal(200),new BigDecimal(200));
	  mockMvc.perform(put("/v1/accounts/transferAmount/"+acId1+"/"+acId1+"/2000"))
	  .andExpect(status().isBadRequest());	  
  }   

  @Test
  public void transferAmount_insufficientBalance() throws Exception {
	  String acId1="Id-123";
	  String acId2="Id-124";	  
	  createSampleAccount(acId1,acId2,new BigDecimal(200),new BigDecimal(200));
	  mockMvc.perform(put("/v1/accounts/transferAmount/"+acId1+"/"+acId2+"/300"))
	  .andExpect(status().isOk())
	  .andExpect(
			  content().string("Insufficient Balance in account "+acId1));
  }   
  
  @Test
  public void transferAmount_fromAccountNotExist() throws Exception {
	  String acId1="Id-123";
	  String acId2="Id-124";	  	
	  mockMvc.perform(put("/v1/accounts/transferAmount/"+acId1+"/"+acId2+"/300"))
	  .andExpect(status().isBadRequest())
	  .andExpect(
			  content().string("Account for account id "+acId1+" does not exists"));
  }   
  
  @Test
  public void transferAmount_toAccountNotExist() throws Exception {
	  String acId1="Id-123";
	  String acId2="Id-124";	  	
	  String acId3="Id-125";	  	
	  createSampleAccount(acId1,acId2,new BigDecimal(200),new BigDecimal(200));
	  mockMvc.perform(put("/v1/accounts/transferAmount/"+acId1+"/"+acId3+"/300"))
	  .andExpect(status().isBadRequest())
	  .andExpect(
			  content().string("Account for account id "+acId3+" does not exists"));
  }   



private void createSampleAccount(String acId1,String acId2,BigDecimal balance1,BigDecimal balance2) {
	 
	  Account account1 = new Account(acId1,balance1);
	  Account account2 = new Account(acId2,balance2);
	  this.accountsService.createAccount(account1);
	  this.accountsService.createAccount(account2);
  }
  
}
