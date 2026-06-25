package com.example.cramschool.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.OperationLog;
import com.example.cramschool.repository.OperationLogRepository;

@Service
public class OperationLogService {

	private final OperationLogRepository operationLogRepository;

	public OperationLogService(OperationLogRepository operationLogRepository) {
		this.operationLogRepository = operationLogRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(Long accountId, Long teacherId, String actorName,
			String action, String method, String path, String result) {
		OperationLog log = new OperationLog();
		log.setAccountId(accountId);
		log.setTeacherId(teacherId);
		log.setActorName(hasText(actorName) ? actorName : "未登入使用者");
		log.setAction(action);
		log.setRequestMethod(method);
		log.setRequestPath(path);
		log.setResult(result);
		operationLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public List<OperationLog> findRecent() {
		return operationLogRepository.findTop500ByOrderByCreatedAtDescIdDesc();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
