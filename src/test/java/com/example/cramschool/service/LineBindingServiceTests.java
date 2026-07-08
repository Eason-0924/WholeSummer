package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.cramschool.dto.LineBindingReply;
import com.example.cramschool.entity.LineBindCode;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.LineBindCodeRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;

class LineBindingServiceTests {

	@Test
	void bindFromLineMessageAcceptsCommandWithoutSpaceBeforeCode() {
		Student student = new Student();
		student.setChineseName("王小明");
		LineBindCode bindCode = new LineBindCode();
		bindCode.setStudent(student);
		bindCode.setRelation("媽媽");
		bindCode.setCode("123456");
		bindCode.setExpiredAt(LocalDateTime.now().plusHours(1));
		LineBindCodeRepository codeRepository = mock(LineBindCodeRepository.class);
		ParentLineBindingRepository bindingRepository = mock(ParentLineBindingRepository.class);
		when(codeRepository.findFirstByCodeAndUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
				any(), any())).thenReturn(Optional.of(bindCode));
		when(bindingRepository.findByStudentAndLineUserId(student, "line-user-1")).thenReturn(Optional.empty());

		LineBindingService service = new LineBindingService(codeRepository, bindingRepository, null, null, null);

		LineBindingReply reply = service.bindFromLineMessage("line-user-1", "家長 LINE", "綁定123456");

		assertThat(reply.handled()).isTrue();
		assertThat(reply.success()).isTrue();
		ArgumentCaptor<ParentLineBinding> bindingCaptor = ArgumentCaptor.forClass(ParentLineBinding.class);
		verify(bindingRepository).save(bindingCaptor.capture());
		ParentLineBinding saved = bindingCaptor.getValue();
		assertThat(saved.getStudent()).isSameAs(student);
		assertThat(saved.getRelation()).isEqualTo("媽媽");
		assertThat(saved.getLineUserId()).isEqualTo("line-user-1");
	}
}
