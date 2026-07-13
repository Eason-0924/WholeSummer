package com.example.cramschool.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.cramschool.dto.data.DataDtos;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.DataManagementService;
import com.example.cramschool.service.DataManagementService.ConcurrentDataModificationException;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin/data")
public class DataManagementApiController {
	private final DataManagementService service;
	private final TeacherPermissionService permissions;

	public DataManagementApiController(DataManagementService service, TeacherPermissionService permissions) {
		this.service = service;
		this.permissions = permissions;
	}

	@GetMapping("/resources")
	public List<DataDtos.Resource> resources() { return service.resources(); }

	@GetMapping("/resources/{key}/metadata")
	public DataDtos.Metadata metadata(@PathVariable String key, HttpSession session) {
		return service.metadata(key, has(session, TeacherPermissionType.DATA_VIEW_SENSITIVE));
	}

	@GetMapping("/resources/{key}/rows")
	public DataDtos.Page rows(@PathVariable String key,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
			@RequestParam(required = false) String sort, @RequestParam(defaultValue = "desc") String direction,
			@RequestParam(required = false) String keyword, HttpSession session) {
		return service.rows(key, page, size, sort, direction, keyword,
				has(session, TeacherPermissionType.DATA_VIEW_SENSITIVE));
	}

	@GetMapping("/resources/{key}/fields/{field}/options")
	public DataDtos.OptionsPage options(@PathVariable String key, @PathVariable String field,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.options(key, field, keyword, page, size);
	}

	@GetMapping("/resources/{parentKey}/rows/{id}/children/{childKey}")
	public DataDtos.Page children(@PathVariable String parentKey, @PathVariable String id,
			@PathVariable String childKey, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, HttpSession session) {
		return service.childRows(parentKey, id, childKey, page, size,
				has(session, TeacherPermissionType.DATA_VIEW_SENSITIVE));
	}

	@DeleteMapping("/resources/{parentKey}/rows/{id}/children/{childKey}/{childId}")
	public ResponseEntity<Void> deleteChild(@PathVariable String parentKey, @PathVariable String id,
			@PathVariable String childKey, @PathVariable String childId, HttpSession session) {
		require(session, TeacherPermissionType.DATA_DELETE);
		service.deleteChild(parentKey, id, childKey, childId, accountId(session),
				teacherId(session), teacherName(session));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/resources/{key}/rows")
	public ResponseEntity<Map<String, Object>> create(@PathVariable String key,
			@RequestBody DataDtos.ValuesRequest request, HttpSession session) {
		require(session, TeacherPermissionType.DATA_EDIT);
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(key, request.values(),
				accountId(session), teacherId(session), teacherName(session)));
	}

	@PutMapping("/resources/{key}/rows/{id}")
	public Map<String, Object> update(@PathVariable String key, @PathVariable String id,
			@RequestBody DataDtos.ValuesRequest request, HttpSession session) {
		require(session, TeacherPermissionType.DATA_EDIT);
		return service.update(key, id, request.values(), request.updatedAt(), accountId(session),
				teacherId(session), teacherName(session));
	}

	@GetMapping("/resources/{key}/rows/{id}/delete-impact")
	public DataDtos.DeleteImpact impact(@PathVariable String key, @PathVariable String id, HttpSession session) {
		require(session, TeacherPermissionType.DATA_DELETE);
		return service.deleteImpact(key, id);
	}

	@DeleteMapping("/resources/{key}/rows/{id}")
	public ResponseEntity<Void> delete(@PathVariable String key, @PathVariable String id, HttpSession session) {
		require(session, TeacherPermissionType.DATA_DELETE);
		service.delete(key, id, accountId(session), teacherId(session), teacherName(session));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/resources/{key}/rows/{id}/hard-delete")
	public ResponseEntity<Void> hardDelete(@PathVariable String key, @PathVariable String id,
			HttpSession session) {
		require(session, TeacherPermissionType.DATA_DELETE);
		service.hardDelete(key, id, accountId(session), teacherId(session), teacherName(session));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(ConcurrentDataModificationException.class)
	public ResponseEntity<DataDtos.ErrorResponse> conflict(ConcurrentDataModificationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new DataDtos.ErrorResponse(false, "CONCURRENT_MODIFICATION", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<DataDtos.ErrorResponse> invalid(IllegalArgumentException ex) {
		String code = ex.getMessage() != null && ex.getMessage().contains("權限") ? "PERMISSION_DENIED" : "VALIDATION_FAILED";
		HttpStatus status = code.equals("PERMISSION_DENIED") ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(new DataDtos.ErrorResponse(false, code, ex.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<DataDtos.ErrorResponse> dataIntegrity(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new DataDtos.ErrorResponse(false, "DATA_IN_USE", "操作失敗：此資料仍被其他紀錄使用，無法刪除。"));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<DataDtos.ErrorResponse> operationFailed(IllegalStateException ex) {
		String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "操作失敗，請查看系統紀錄。" : ex.getMessage();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new DataDtos.ErrorResponse(false, "OPERATION_FAILED", "操作失敗：" + message));
	}

	private void require(HttpSession session, TeacherPermissionType permission) {
		if (!has(session, permission)) throw new IllegalArgumentException("您沒有執行此操作的權限");
	}
	private boolean has(HttpSession session, TeacherPermissionType permission) { return permissions.hasPermission(teacherId(session), permission); }
	private Long teacherId(HttpSession session) { Object v = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY); return v instanceof Long id ? id : null; }
	private Long accountId(HttpSession session) { Object v = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY); return v instanceof Long id ? id : null; }
	private String teacherName(HttpSession session) { Object v = session.getAttribute(AuthController.TEACHER_NAME_SESSION_KEY); return v == null ? "" : v.toString(); }
}
