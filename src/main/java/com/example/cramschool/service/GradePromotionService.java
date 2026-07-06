package com.example.cramschool.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Student;
import com.example.cramschool.form.GradePromotionDraft;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class GradePromotionService {

	public static final String ACTION_GRADUATE = "GRADUATE";
	public static final String ACTION_PROMOTE = "PROMOTE";
	public static final String ACTION_GRADE_ONLY = "GRADE_ONLY";

	private static final Map<String, String> NEXT_GRADE = Map.of(
			"國一", "國二",
			"國二", "國三",
			"國三", "高一",
			"高一", "高二",
			"高二", "高三");

	private final StudentRepository studentRepository;
	private final ClassRoomRepository classRoomRepository;
	private final ClassStudentRepository classStudentRepository;

	public GradePromotionService(StudentRepository studentRepository, ClassRoomRepository classRoomRepository,
			ClassStudentRepository classStudentRepository) {
		this.studentRepository = studentRepository;
		this.classRoomRepository = classRoomRepository;
		this.classStudentRepository = classStudentRepository;
	}

	@Transactional(readOnly = true)
	public List<StudentOption> findTerminalStudents() {
		return studentRepository.findByActiveTrue().stream()
				.filter(student -> "國三".equals(student.getGrade()) || "高三".equals(student.getGrade()))
				.sorted(studentComparator())
				.map(student -> new StudentOption(student.getId(), student.getDisplayName(), student.getGrade(),
						NEXT_GRADE.get(student.getGrade())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ClassOption> findPromotableClasses() {
		return classRoomRepository.findByActiveTrue().stream()
				.filter(classRoom -> NEXT_GRADE.containsKey(classRoom.getGrade()))
				.sorted(Comparator.comparingInt((ClassRoom classRoom) -> gradeOrder(classRoom.getGrade()))
						.thenComparing(ClassRoom::getDisplayName))
				.map(classRoom -> new ClassOption(classRoom.getId(), classRoom.getDisplayName(),
						nextClassName(classRoom), classRoom.getGrade(), NEXT_GRADE.get(classRoom.getGrade())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ClassMemberOption> findSelectedClassMembers(GradePromotionDraft draft) {
		List<ClassMemberOption> result = new ArrayList<>();
		for (Long classId : draft.getPromotedClassIds()) {
			ClassRoom classRoom = classRoomRepository.findById(classId)
					.orElseThrow(() -> new IllegalArgumentException("找不到要升級的班級"));
			List<MemberOption> members = classStudentRepository
					.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(classId).stream()
					.map(ClassStudent::getStudent)
					.map(student -> new MemberOption(student.getId(), student.getDisplayName(), student.getGrade(),
							willRemainActive(student, draft)))
					.toList();
			result.add(new ClassMemberOption(classId, classRoom.getDisplayName(), nextClassName(classRoom), members));
		}
		return result;
	}

	public PromotionResult complete(GradePromotionDraft draft) {
		validateTerminalActions(draft);

		List<Student> activeStudents = studentRepository.findByActiveTrue();
		Map<Long, Student> activeStudentsById = new LinkedHashMap<>();
		for (Student student : activeStudents) {
			activeStudentsById.put(student.getId(), student);
			promoteStudent(student, draft);
		}

		int createdClassCount = 0;
		int updatedClassGradeCount = 0;
		int joinedStudentCount = 0;
		for (Long classId : draft.getGradeOnlyClassIds()) {
			ClassRoom classRoom = classRoomRepository.findById(classId)
					.orElseThrow(() -> new IllegalArgumentException("找不到要升級的班級"));
			String nextGrade = NEXT_GRADE.get(classRoom.getGrade());
			if (!classRoom.isActive() || nextGrade == null) {
				throw new IllegalArgumentException("班級已結束或無法再升級：" + classRoom.getDisplayName());
			}
			classRoom.setGrade(nextGrade);
			classRoomRepository.save(classRoom);
			updatedClassGradeCount++;
		}
		for (Long classId : draft.getPromotedClassIds()) {
			ClassRoom oldClass = classRoomRepository.findById(classId)
					.orElseThrow(() -> new IllegalArgumentException("找不到要升級的班級"));
			String nextGrade = NEXT_GRADE.get(oldClass.getGrade());
			if (!oldClass.isActive() || nextGrade == null) {
				throw new IllegalArgumentException("班級已結束或無法再升級：" + oldClass.getDisplayName());
			}

			ClassRoom newClass = cloneAsNextGrade(oldClass, nextGrade);
			oldClass.setActive(false);
			classRoomRepository.save(oldClass);
			classRoomRepository.save(newClass);
			createdClassCount++;

			Set<Long> selectedStudentIds = draft.getJoinedStudentIdsByClass()
					.getOrDefault(classId, Set.of());
			Set<Long> originalStudentIds = classStudentRepository
					.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(classId).stream()
					.map(classStudent -> classStudent.getStudent().getId())
					.collect(java.util.stream.Collectors.toSet());
			for (Long studentId : selectedStudentIds) {
				Student student = activeStudentsById.get(studentId);
				if (student == null || !student.isActive() || !originalStudentIds.contains(studentId)) {
					continue;
				}
				ClassStudent membership = new ClassStudent();
				membership.setClassRoom(newClass);
				membership.setStudent(student);
				membership.setActive(true);
				classStudentRepository.save(membership);
				joinedStudentCount++;
			}
		}

		return new PromotionResult(activeStudents.size(), createdClassCount, updatedClassGradeCount,
				joinedStudentCount);
	}

	public void validateTerminalActions(GradePromotionDraft draft) {
		for (StudentOption student : findTerminalStudents()) {
			String action = draft.getTerminalStudentActions().get(student.id());
			if ("高三".equals(student.grade()) && !ACTION_GRADUATE.equals(action)) {
				throw new IllegalArgumentException(student.displayName() + " 為高三學生，升年級後必須設為畢業");
			}
			if ("國三".equals(student.grade())
					&& !ACTION_GRADUATE.equals(action) && !ACTION_PROMOTE.equals(action)) {
				throw new IllegalArgumentException("請選擇 " + student.displayName() + " 的升級方式");
			}
			if ("國三".equals(student.grade()) && ACTION_PROMOTE.equals(action)
					&& !hasText(draft.getPromotedStudentSchools().get(student.id()))) {
				throw new IllegalArgumentException("請輸入 " + student.displayName() + " 升高一後的新學校");
			}
		}
	}

	private void promoteStudent(Student student, GradePromotionDraft draft) {
		String grade = student.getGrade();
		if ("國三".equals(grade) || "高三".equals(grade)) {
			String action = draft.getTerminalStudentActions().get(student.getId());
			if ("高三".equals(grade) || ACTION_GRADUATE.equals(action)) {
				student.setActive(false);
			} else if (ACTION_PROMOTE.equals(action)) {
				student.setGrade(NEXT_GRADE.get(grade));
				student.setSchool(draft.getPromotedStudentSchools().get(student.getId()).trim());
			}
			return;
		}
		String nextGrade = NEXT_GRADE.get(grade);
		if (nextGrade != null) {
			student.setGrade(nextGrade);
		}
	}

	private boolean willRemainActive(Student student, GradePromotionDraft draft) {
		if (!student.isActive()) {
			return false;
		}
		if ("國三".equals(student.getGrade()) || "高三".equals(student.getGrade())) {
			if ("高三".equals(student.getGrade())) {
				return false;
			}
			return ACTION_PROMOTE.equals(draft.getTerminalStudentActions().get(student.getId()));
		}
		return true;
	}

	private ClassRoom cloneAsNextGrade(ClassRoom oldClass, String nextGrade) {
		ClassRoom newClass = new ClassRoom();
		newClass.setGrade(nextGrade);
		newClass.setSubject(oldClass.getSubject());
		newClass.setClassType(oldClass.getClassType());
		newClass.setTeacher(oldClass.getTeacher());
		newClass.setDescription(oldClass.getDescription());
		newClass.setActive(true);
		for (ClassSchedule schedule : oldClass.getEffectiveSchedules()) {
			newClass.addSchedule(new ClassSchedule(schedule.getWeekday(), schedule.getStartTime(), schedule.getEndTime()));
		}
		return newClass;
	}

	private String nextClassName(ClassRoom classRoom) {
		String nextGrade = NEXT_GRADE.get(classRoom.getGrade());
		String baseName = (nextGrade == null ? classRoom.getGrade() : nextGrade) + classRoom.getSubjectName();
		if (classRoom.getClassType() != null && !classRoom.getClassType().isBlank()) {
			return baseName + "（" + classRoom.getClassType().trim() + "）";
		}
		return baseName;
	}

	private Comparator<Student> studentComparator() {
		return Comparator.comparingInt((Student student) -> gradeOrder(student.getGrade()))
				.thenComparing(Student::getChineseName, Comparator.nullsLast(String::compareTo));
	}

	private int gradeOrder(String grade) {
		int index = SchoolOptions.STUDENT_GRADES.indexOf(grade);
		return index >= 0 ? index : SchoolOptions.STUDENT_GRADES.size();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public record StudentOption(Long id, String displayName, String grade, String promotedGrade) {
	}

	public record ClassOption(Long id, String currentName, String promotedName, String grade, String promotedGrade) {
	}

	public record MemberOption(Long id, String displayName, String grade, boolean defaultJoin) {
	}

	public record ClassMemberOption(Long classId, String currentName, String promotedName, List<MemberOption> members) {
	}

	public record PromotionResult(int promotedStudentCount, int createdClassCount, int updatedClassGradeCount,
			int joinedStudentCount) {
	}
}
