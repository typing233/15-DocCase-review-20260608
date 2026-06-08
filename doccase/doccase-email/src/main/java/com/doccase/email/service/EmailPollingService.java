package com.doccase.email.service;

import com.doccase.email.domain.entity.EmailAccount;
import com.doccase.email.domain.entity.EmailArchiveRecord;

public interface EmailPollingService {

    void pollAccount(EmailAccount account);

    void pollAllAccounts();

    void retryRecord(EmailArchiveRecord record);
}
