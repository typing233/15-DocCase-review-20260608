package com.doccase.email.service;

import com.doccase.email.domain.entity.EmailAccount;

public interface EmailPollingService {

    void pollAccount(EmailAccount account);

    void pollAllAccounts();
}
