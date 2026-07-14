package com.example.cramschool.service.system;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.system.SystemLogDto;
import com.example.cramschool.repository.OperationLogRepository;

@Service
public class SystemLogService {

	private static final int DEFAULT_PAGE_SIZE = 30;
	private static final int MAX_PAGE_SIZE = 100;
	private final OperationLogRepository operationLogRepository;

	public SystemLogService(OperationLogRepository operationLogRepository) {
		this.operationLogRepository = operationLogRepository;
	}

	@Transactional(readOnly = true)
	public Page<SystemLogDto> findPage(int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return operationLogRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(safePage, safeSize))
				.map(log -> new SystemLogDto(log.getId(), log.getCreatedAt(), log.getActorName(), log.getAction(),
						log.getRequestMethod(), log.getRequestPath(), log.getResult()));
	}

	@Transactional(readOnly = true)
	public List<SystemLogDto> findRecent(int limit) {
		return operationLogRepository.findTop500ByOrderByCreatedAtDescIdDesc().stream()
				.limit(Math.max(0, Math.min(limit, 100)))
				.map(log -> new SystemLogDto(log.getId(), log.getCreatedAt(), log.getActorName(), log.getAction(),
						log.getRequestMethod(), log.getRequestPath(), log.getResult()))
				.toList();
	}
}
