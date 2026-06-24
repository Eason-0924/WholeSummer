-- MySQL dump 10.13  Distrib 9.6.0, for macos15 (arm64)
--
-- Host: localhost    Database: WholeSummer
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'f7437970-237e-11f1-b3f1-625ca62ef236:1-72748';

--
-- Table structure for table `backup_records`
--

DROP TABLE IF EXISTS `backup_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `backup_time` datetime(6) NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('FAILED','SUCCESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_records`
--

LOCK TABLES `backup_records` WRITE;
/*!40000 ALTER TABLE `backup_records` DISABLE KEYS */;
INSERT INTO `backup_records` VALUES (1,'2026-06-24 01:16:54.270325','WholeSummer_backup_20260624_011654.sql','data/backups/WholeSummer_backup_20260624_011654.sql',0,'無法建立備份：Cannot run program \"mysqldump\": Exec failed, error: 2 (No such file or directory) ','FAILED'),(2,'2026-06-24 01:19:13.392143','WholeSummer_backup_20260624_011913.sql','data/backups/WholeSummer_backup_20260624_011913.sql',1026,'mysqldump 執行失敗，代碼：2','FAILED');
/*!40000 ALTER TABLE `backup_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_schedules`
--

DROP TABLE IF EXISTS `class_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `end_time` time NOT NULL,
  `start_time` time NOT NULL,
  `weekday` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKd4fidccuckfhabn3u91coqo6g` (`class_id`),
  CONSTRAINT `FKd4fidccuckfhabn3u91coqo6g` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_schedules`
--

LOCK TABLES `class_schedules` WRITE;
/*!40000 ALTER TABLE `class_schedules` DISABLE KEYS */;
INSERT INTO `class_schedules` VALUES (1,'21:00:00','19:00:00','星期二',7),(2,'20:30:00','18:30:00','星期六',7),(3,'21:00:00','19:00:00','星期四',10),(4,'20:30:00','18:30:00','星期六',9),(5,'17:30:00','14:30:00','星期六',6),(6,'21:00:00','19:00:00','星期二',8),(7,'21:00:00','19:00:00','星期四',11),(8,'12:00:00','10:00:00','星期一',12),(9,'20:00:00','18:00:00','星期三',12);
/*!40000 ALTER TABLE `class_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_students`
--

DROP TABLE IF EXISTS `class_students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `joined_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_student` (`class_id`,`student_id`),
  KEY `FK8x4jwkaf3emayhwuqidfr5w0c` (`student_id`),
  CONSTRAINT `FK8x4jwkaf3emayhwuqidfr5w0c` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKjuh9br5vimkw71ko8qyswp3ci` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_students`
--

LOCK TABLES `class_students` WRITE;
/*!40000 ALTER TABLE `class_students` DISABLE KEYS */;
INSERT INTO `class_students` VALUES (7,_binary '','2026-06-23 15:19:41.866318','2026-06-23 15:19:41.866318','2026-06-23 15:19:41.866318',7,8),(8,_binary '','2026-06-23 15:19:45.460512','2026-06-23 15:19:45.460512','2026-06-23 15:19:45.460512',7,6),(10,_binary '','2026-06-23 15:20:13.498453','2026-06-23 15:20:13.498453','2026-06-23 15:20:13.498453',9,4),(11,_binary '','2026-06-23 15:20:17.500406','2026-06-23 15:20:17.500406','2026-06-23 15:20:17.500406',9,3),(12,_binary '','2026-06-23 15:20:52.415821','2026-06-23 15:20:52.415821','2026-06-23 15:20:52.415821',6,5),(13,_binary '','2026-06-23 15:20:56.447926','2026-06-23 15:20:56.447926','2026-06-23 15:20:56.447926',6,4),(14,_binary '','2026-06-23 15:20:59.850859','2026-06-23 15:20:59.850859','2026-06-23 15:20:59.850859',6,3),(15,_binary '','2026-06-23 15:21:12.179492','2026-06-23 15:21:12.179492','2026-06-23 15:21:12.179492',8,4),(16,_binary '','2026-06-23 15:21:18.358922','2026-06-23 15:21:18.358922','2026-06-23 15:21:18.358922',8,3),(17,_binary '','2026-06-23 15:21:30.587382','2026-06-23 15:21:30.587382','2026-06-23 15:21:30.587382',10,7),(18,_binary '','2026-06-23 15:21:34.542668','2026-06-23 15:21:34.542668','2026-06-23 15:21:34.542668',10,9),(19,_binary '','2026-06-24 01:14:23.856860','2026-06-24 01:14:23.856860','2026-06-24 01:14:23.856860',12,10);
/*!40000 ALTER TABLE `class_students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `class_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grade` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `teacher` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subject_id` bigint DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcaops3x4cgf4peavqc3gh87k1` (`subject_id`),
  KEY `FK8td8h5k21lq8jax2h6oobm9l0` (`teacher_id`),
  CONSTRAINT `FK8td8h5k21lq8jax2h6oobm9l0` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FKcaops3x4cgf4peavqc3gh87k1` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (6,_binary '','2026-06-23 14:48:50.532509','','2026-06-23 14:48:50.532509','','高二',NULL,3,2),(7,_binary '','2026-06-23 14:51:48.727748','','2026-06-23 14:51:48.727748','','高一',NULL,3,2),(8,_binary '','2026-06-23 14:52:20.978867','','2026-06-23 14:52:20.978867','','高二',NULL,5,2),(9,_binary '','2026-06-23 14:52:45.525281','','2026-06-23 14:52:45.525281','','高二',NULL,6,2),(10,_binary '','2026-06-23 14:53:30.643945','','2026-06-23 14:53:30.643945','','國二',NULL,3,2),(11,_binary '','2026-06-23 15:09:11.775773','','2026-06-23 15:09:11.775773','','國一',NULL,7,2),(12,_binary '','2026-06-24 01:14:19.143943','班級多日上課測試','2026-06-24 01:14:19.143943','測試班','國二',NULL,8,3);
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exams`
--

DROP TABLE IF EXISTS `exams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exam_date` date NOT NULL,
  `full_score` int NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `class_id` bigint NOT NULL,
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkq9895f263nw6qxibikek0t40` (`class_id`),
  KEY `FKopre4n7j7fpxqbtbwpv8ywn1y` (`subject_id`),
  CONSTRAINT `FKkq9895f263nw6qxibikek0t40` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `FKopre4n7j7fpxqbtbwpv8ywn1y` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exams`
--

LOCK TABLES `exams` WRITE;
/*!40000 ALTER TABLE `exams` DISABLE KEYS */;
INSERT INTO `exams` VALUES (4,'2026-06-23 20:41:59.817956','','2026-06-23',0,'段考範圍複習','2026-06-23 20:41:59.817956',8,5),(5,'2026-06-24 01:15:39.939782','測驗測試','2026-06-24',100,'瀏覽器測試測驗','2026-06-24 01:15:39.939782',12,8),(6,'2026-06-24 01:15:59.589367','課堂練習測試','2026-06-24',0,'瀏覽器測試課堂練習','2026-06-24 01:15:59.589367',12,8);
/*!40000 ALTER TABLE `exams` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `homework_records`
--

DROP TABLE IF EXISTS `homework_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `status` enum('EXCUSED','LATE','NOT_SUBMITTED','SUBMITTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `teacher_comment` text COLLATE utf8mb4_unicode_ci,
  `updated_at` datetime(6) NOT NULL,
  `homework_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_student` (`homework_id`,`student_id`),
  KEY `FKphnbydk4e0bcvxh7s7dhvsgof` (`student_id`),
  CONSTRAINT `FKb2ridvj246050ub9j01h1w4xf` FOREIGN KEY (`homework_id`) REFERENCES `homeworks` (`id`),
  CONSTRAINT `FKphnbydk4e0bcvxh7s7dhvsgof` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `homework_records`
--

LOCK TABLES `homework_records` WRITE;
/*!40000 ALTER TABLE `homework_records` DISABLE KEYS */;
INSERT INTO `homework_records` VALUES (3,'2026-06-24 01:15:02.519916','SUBMITTED','2026-06-24 01:15:21.513738','已完成測試','2026-06-24 01:15:21.513972',2,10);
/*!40000 ALTER TABLE `homework_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `homeworks`
--

DROP TABLE IF EXISTS `homeworks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homeworks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `due_date` date NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `class_id` bigint NOT NULL,
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbfqdgtdbcrfhh4qou59cs8c0a` (`class_id`),
  KEY `FKm0mfhlsryrr7qxvc8dmab648k` (`subject_id`),
  CONSTRAINT `FKbfqdgtdbcrfhh4qou59cs8c0a` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `FKm0mfhlsryrr7qxvc8dmab648k` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `homeworks`
--

LOCK TABLES `homeworks` WRITE;
/*!40000 ALTER TABLE `homeworks` DISABLE KEYS */;
INSERT INTO `homeworks` VALUES (2,'2026-06-24','2026-06-24 01:15:02.509921','作業備註測試','2026-06-24','瀏覽器測試作業','2026-06-24 01:15:02.509921',12,8);
/*!40000 ALTER TABLE `homeworks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scores`
--

DROP TABLE IF EXISTS `scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `score` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `exam_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_student` (`exam_id`,`student_id`),
  KEY `FKpmp9k9d20q6euqo2g35a00wyl` (`student_id`),
  CONSTRAINT `FK32g8we32gx474l7jy6sl0vy3` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`),
  CONSTRAINT `FKpmp9k9d20q6euqo2g35a00wyl` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scores`
--

LOCK TABLES `scores` WRITE;
/*!40000 ALTER TABLE `scores` DISABLE KEYS */;
INSERT INTO `scores` VALUES (5,'','2026-06-23 20:56:14.844383',1,'2026-06-23 20:56:14.844383',4,3),(6,'','2026-06-23 20:56:14.850271',1,'2026-06-23 20:56:14.850271',4,4),(7,'分數測試','2026-06-24 01:15:52.240886',88,'2026-06-24 01:15:52.240886',5,10),(8,'完成狀態測試','2026-06-24 01:16:27.645048',1,'2026-06-24 01:16:27.645048',6,10);
/*!40000 ALTER TABLE `scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_attendances`
--

DROP TABLE IF EXISTS `student_attendances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_attendances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attendance_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ABSENT','LATE','LEAVE','PRESENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKne8vmuwfheohr8h284w03kkky` (`student_id`,`class_id`,`attendance_date`),
  KEY `FKja5k7vsd5ihwbfrmlobpjvjy3` (`class_id`),
  CONSTRAINT `FK8o64c9gsmi8bnsla50gwb91vt` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKja5k7vsd5ihwbfrmlobpjvjy3` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_attendances`
--

LOCK TABLES `student_attendances` WRITE;
/*!40000 ALTER TABLE `student_attendances` DISABLE KEYS */;
INSERT INTO `student_attendances` VALUES (1,'2026-06-23','2026-06-23 20:22:04.356032','','PRESENT','2026-06-23 20:27:47.272805',8,3),(2,'2026-06-23','2026-06-23 20:22:04.376906','準時','PRESENT','2026-06-23 20:35:56.570369',8,4),(3,'2026-06-24','2026-06-24 01:14:47.827649','點名測試備註','LATE','2026-06-24 01:14:47.827649',12,10);
/*!40000 ALTER TABLE `student_attendances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `gender` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grade` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `school` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `chinese_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `english_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (3,_binary '','2026-06-23 15:10:32.046741','男','高二','雙胞胎弟弟\r\n','','新莊高中','2026-06-23 15:10:43.568078','王致?','Lucas'),(4,_binary '','2026-06-23 15:11:16.337561','男','高二','雙胞胎哥哥','','林口高中','2026-06-23 15:11:16.337561','王致？','Oscar'),(5,_binary '','2026-06-23 15:12:43.223003','男','高二','','','中正高中','2026-06-23 15:12:43.223003','李汯鍏','Nick'),(6,_binary '','2026-06-23 15:15:53.940546','男','高一','','','樹林高中','2026-06-23 15:15:53.940546','魚宗源','Fish'),(7,_binary '','2026-06-23 15:16:40.542208','女','國二','','','義學國中','2026-06-23 15:16:40.542208','ㄩˋㄒㄧ ','Hebe'),(8,_binary '','2026-06-23 15:17:49.406679','男','高一','','','恆毅高中','2026-06-23 15:17:49.406679','林承億','Jerry'),(9,_binary '','2026-06-23 15:18:58.575398','男','國二','','','義學國中','2026-06-23 15:18:58.575398','簡建天','Jeremy'),(10,_binary '','2026-06-24 01:12:27.833169','男','國二','編輯測試','0911111111','測試學校','2026-06-24 01:12:45.851804','測試學生瀏覽器修正','BrowserTestEdit');
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subject_teachers`
--

DROP TABLE IF EXISTS `subject_teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subject_teachers` (
  `subject_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`subject_id`,`teacher_id`),
  KEY `FKnphdxlk8ekqt4uuodnwl6nmcc` (`teacher_id`),
  CONSTRAINT `FKnnu8waglyumx966dh1gkcnkd4` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`),
  CONSTRAINT `FKnphdxlk8ekqt4uuodnwl6nmcc` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subject_teachers`
--

LOCK TABLES `subject_teachers` WRITE;
/*!40000 ALTER TABLE `subject_teachers` DISABLE KEYS */;
INSERT INTO `subject_teachers` VALUES (3,2),(4,2),(5,2),(6,2),(7,2),(8,3);
/*!40000 ALTER TABLE `subject_teachers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subjects`
--

DROP TABLE IF EXISTS `subjects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `grade_levels` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `teacher` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subjects`
--

LOCK TABLES `subjects` WRITE;
/*!40000 ALTER TABLE `subjects` DISABLE KEYS */;
INSERT INTO `subjects` VALUES (1,_binary '','2026-06-22 15:10:02.931472','','國文','2026-06-22 15:26:50.118676','國一,國二,國三,高一,高二,高三','國文老師'),(2,_binary '','2026-06-22 15:10:15.876467','','英文','2026-06-22 15:26:58.275760','國一,國二,國三,高一,高二,高三','Cindy'),(3,_binary '','2026-06-22 15:10:22.277870','','數學','2026-06-23 14:46:56.259986','國一,國二,國三,高一,高二,高三','Eason'),(4,_binary '','2026-06-22 15:20:48.514837','','理化','2026-06-23 14:47:01.177912','國一,國二,國三','Eason'),(5,_binary '','2026-06-22 15:20:58.325653','','物理','2026-06-23 14:47:05.751545','高一,高二,高三','Eason'),(6,_binary '','2026-06-22 15:21:06.447588','','化學','2026-06-23 14:47:09.759094','高一,高二,高三','Eason'),(7,_binary '','2026-06-23 15:07:36.747645','當日科目由老師依照進度決定','數理','2026-06-23 15:32:36.360688','國一',NULL),(8,_binary '','2026-06-24 01:13:34.893486','科目編輯測試','測試科目瀏覽器修正','2026-06-24 01:14:01.217438','國二',NULL);
/*!40000 ALTER TABLE `subjects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_settings`
--

DROP TABLE IF EXISTS `system_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `setting_value` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnm18l4pyovtvd8y3b3x0l2y64` (`setting_key`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_settings`
--

LOCK TABLES `system_settings` WRITE;
/*!40000 ALTER TABLE `system_settings` DISABLE KEYS */;
INSERT INTO `system_settings` VALUES (1,'AUTH_PASSWORD_SALT','52c481e8f5fecbed28f7d23d177e4dd0','2026-06-24 01:16:45.161647'),(2,'AUTH_PASSWORD_HASH','cec2808952fc17a6d0a278227e2bdd03e281c258238ba9319389ac98a109d3f3','2026-06-24 01:16:45.163295'),(3,'THEME_MODE','dark','2026-06-24 01:16:35.293857'),(4,'HOMEWORK_WARNING_DAYS','7','2026-06-24 01:16:35.298475'),(5,'SYSTEM_NAME','霍爾夏天補習班 Whole Summer','2026-06-24 00:54:17.131412'),(6,'BACKUP_REMINDER_DAYS','14','2026-06-24 01:16:35.300429');
/*!40000 ALTER TABLE `system_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_attendances`
--

DROP TABLE IF EXISTS `teacher_attendances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_attendances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clock_in_time` time DEFAULT NULL,
  `clock_out_time` time DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `attendance_date` date NOT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ABSENT','LEAVE','WORKING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5li2wu9x399kv3byoxjnw4j7m` (`teacher_id`,`attendance_date`),
  CONSTRAINT `FKpycv944lubxsxrut90t956yki` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_attendances`
--

LOCK TABLES `teacher_attendances` WRITE;
/*!40000 ALTER TABLE `teacher_attendances` DISABLE KEYS */;
INSERT INTO `teacher_attendances` VALUES (1,'09:00:00','17:30:00','2026-06-24 01:13:19.033074','2026-06-24','出勤測試','WORKING','2026-06-24 01:13:19.033074',3);
/*!40000 ALTER TABLE `teacher_attendances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teachers`
--

DROP TABLE IF EXISTS `teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hire_date` date DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','LEFT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teachers`
--

LOCK TABLES `teachers` WRITE;
/*!40000 ALTER TABLE `teachers` DISABLE KEYS */;
INSERT INTO `teachers` VALUES (2,'2026-06-23 14:46:37.277405','aassddlee0924@gmail.com','2024-05-02','李展宇','Eason','','0958502302','ACTIVE','2026-06-23 14:46:37.277405'),(3,'2026-06-24 01:12:51.036699','teacher-test-edit@example.com','2026-06-24','測試老師瀏覽器修正','測師2','教師編輯測試','0900333444','ACTIVE','2026-06-24 01:13:07.368161');
/*!40000 ALTER TABLE `teachers` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-24  1:21:29
